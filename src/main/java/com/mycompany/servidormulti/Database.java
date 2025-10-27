package com.mycompany.servidormulti;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Database {

    private static String DB_URL = "jdbc:postgresql://localhost:5432/chat_db";
    private static String DB_USER = "postgres";
    private static String DB_PASSWORD = "Quieroserescritora10";

    private static Connection conexion;


    public static void inicializar(){
        try{
            cargarConfiguracion();
            Class.forName("org.postgresql.Driver");

            conexion = DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD);

            crearTablaUsuarios();

            System.out.println("Conectado exitosamente a la base de datos");
            System.out.println("Base de datos: " + DB_URL);
        }catch(ClassNotFoundException e) {
            System.err.println("No se encontro el Driver de la base de datos");
            System.out.println("Funcionando en modo offline, sin base de datos");
        }catch(SQLException e){
            System.err.println("Error al conectar la base de datos: "+e.getMessage());
            System.out.println("Verifica que PostgreSQL este ejecutandose y la configuracion sea correcta");
            System.out.println("Funcionando en modo offline, sin base de datos");
            conexion = null;
        }
    }

    private static void cargarConfiguracion(){
        try(FileInputStream fis = new FileInputStream("database.properties")){
            Properties prop = new Properties();
            prop.load(fis);
            DB_URL = prop.getProperty("DB_URL",DB_URL);
            DB_USER = prop.getProperty("DB_USER",DB_USER);
            DB_PASSWORD = prop.getProperty("DB_PASSWORD",DB_PASSWORD);

            System.out.println("Configuracion cargada desde database.properties");
        }catch(IOException e){
            System.err.println("Usando la configuracion por defecto (database.properties no encontrado)");
        }
    }

    private static void crearTablaUsuarios() throws SQLException{
        String sql = """
                CREATE TABLE IF NOT EXISTS usuarios(
                id SERIAL PRIMARY KEY,
                username varchar(50) UNIQUE NOT NULL,
                password varchar(255) NOT NULL
                )
                """;

        try(Statement stmt = conexion.createStatement()){
            stmt.execute(sql);
            System.out.println("Tabla usuarios creada");
        }
    }

    public static boolean registrarUsuario(String username, String password){
        if(conexion == null){
            return SistemaAutenticacion.registrarUsuarioOffline(username,password);
        }

        String sql = "INSERT INTO usuarios(username, password) VALUES(?,?)";

        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1,username);
            pstmt.setString(2,password);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }catch(SQLException e){
            if(e.getSQLState().equals("23505")){
                return false;
            }
            System.err.println("Error al registrar el usuario: "+e.getMessage());
            return false;
        }
    }

    public static boolean validarLogin(String username, String password){
        if(conexion == null){
            return SistemaAutenticacion.validarLoginOffline(username,password);
        }

        String sql = "SELECT password FROM usuarios WHERE username = ?";

        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1,username);

            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    String contraGuardada = rs.getString("password");
                    return password.equals(contraGuardada);
                }
                return false;
            }
        }catch(SQLException e){
            System.err.println("Error al validar el login: "+e.getMessage());
            return false;
        }
    }

    public static boolean existeUsuario(String username){
        if(conexion == null){
            return SistemaAutenticacion.existeUsuarioOffline(username);
        }

        String sql = "SELECT 1 FROM usuarios WHERE username = ?";

        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1,username);

            try(ResultSet rt = pstmt.executeQuery()){
                return rt.next();
            }
        }catch(SQLException e ){
            System.err.println("Error al obtener el usuario: "+e.getMessage());
            return false;
        }
    }

    public static void cerrarConexion(){
        if(conexion != null){
            try{
                conexion.close();
                System.out.println("Conexion cerrada");
            }catch(SQLException e){
                System.err.println("Error al cerrar el conexion: "+e.getMessage());
            }
        }
    }

    public static boolean estaConectado(){
        return conexion != null;
    }
}
