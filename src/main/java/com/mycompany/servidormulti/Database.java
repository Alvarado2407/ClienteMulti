package com.mycompany.servidormulti;

import java.sql.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private static String DB_URL = "jdbc:postgresql://localhost:5432/clienteMulti";
    private static String DB_USER = "postgres";
    private static String DB_PASSWORD = "admin";

    private static Connection conexion;


    public static void inicializar(){
        try{
            cargarConfiguracion();
            Class.forName("org.postgresql.Driver");

            conexion = DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD);

            crearTablaUsuarios();
            crearTablaEstadisticas();

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

    private static void crearTablaEstadisticas() throws SQLException{
        String sql = """
                CREATE TABLE IF NOT EXISTS estadisticas(
                id SERIAL PRIMARY KEY,
                username varchar(50) UNIQUE NOT NULL,
                victorias INT DEFAULT 0,
                empates INT DEFAULT 0,
                derrotas INT DEFAULT 0,
                puntos INT DEFAULT 0,
                FOREIGN KEY (username) REFERENCES usuarios(username) ON DELETE CASCADE
                )
                """;

        try(Statement stmt = conexion.createStatement()){
            stmt.execute(sql);
            System.out.println("Tabla estadisticas creada");
        }
        crearTablaGrupos();
        crearTablaMiembrosGrupo();
        crearTablaMensajesGrupo();
        crearTablaBloqueos();
    }

    private static void crearTablaBloqueos() throws SQLException{
        String sql = """
                CREATE TABLE IF NOT EXISTS bloqueos(
                id SERIAL PRIMARY KEY,
                bloqueador varchar(50) NOT NULL,
                bloqueado varchar(50) NOT NULL,
                fecha_bloqueo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                unique(bloqueador, bloqueado),
                FOREIGN KEY (bloqueador) REFERENCES usuarios(username) ON DELETE CASCADE,
                FOREIGN KEY (bloqueado) REFERENCES usuarios(username) ON DELETE CASCADE
                )
                """;
        try(Statement stmt = conexion.createStatement()){
            stmt.execute(sql);
            System.out.println("Tabla bloqueos creada");
        }
    }

    private static void crearTablaGrupos() throws SQLException{
        String sql = """
                CREATE TABLE IF NOT EXISTS grupos(
                id SERIAL PRIMARY KEY,
                nombre varchar(50) UNIQUE NOT NULL,
                creador varchar(50),
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try(Statement stmt = conexion.createStatement()){
            stmt.execute(sql);
            System.out.println("Tabla grupos creada");
        }
    }

    private static void crearTablaMiembrosGrupo() throws SQLException{
        String sql = """
                CREATE TABLE IF NOT EXISTS miembros_grupo(
                id SERIAL PRIMARY KEY,
                grupo_nombre varchar(50) NOT NULL,
                username varchar(50) NOT NULL,
                fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(grupo_nombre, username)
                )
                """;

        try(Statement stmt = conexion.createStatement()){
            stmt.execute(sql);
            System.out.println("Tabla miembros_grupo creada");
        }
    }

    private static void crearTablaMensajesGrupo() throws SQLException{
        String sql = """
                CREATE TABLE IF NOT EXISTS mensajes_grupo(
                id SERIAL PRIMARY KEY,
                grupo_nombre varchar(50) NOT NULL,
                username varchar(50) NOT NULL,
                mensaje TEXT NOT NULL,
                fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try(Statement stmt = conexion.createStatement()){
            stmt.execute(sql);
            System.out.println("Tabla mensajes_grupo creada");
        }
    }

    public static boolean crearGrupoSiNoExiste(String nombreGrupo){
        if(conexion == null) return false;

        if(existeGrupo(nombreGrupo)){
            return true;
        }

        return crearGrupo(nombreGrupo, null);
    }

    public static boolean crearGrupo(String nombreGrupo, String creador){
        if(conexion == null) return false;

        String sql = "INSERT INTO grupos(nombre, creador) VALUES(?,?)";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, nombreGrupo);
            pstmt.setString(2, creador);
            return pstmt.executeUpdate() > 0;
        }catch(SQLException e){
            System.err.println("Error creando grupo: " + e.getMessage());
            return false;
        }
    }

    public static boolean existeGrupo(String nombreGrupo){
        if(conexion == null) return false;

        String sql = "SELECT 1 FROM grupos WHERE nombre = ?";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }catch(SQLException e){
            System.err.println("Error verificando grupo: " + e.getMessage());
            return false;
        }
    }

    public static boolean eliminarGrupo(String nombreGrupo){
        if(conexion == null) return false;

        try{
            // Eliminar mensajes
            String sql1 = "DELETE FROM mensajes_grupo WHERE grupo_nombre = ?";
            try(PreparedStatement pstmt = conexion.prepareStatement(sql1)){
                pstmt.setString(1, nombreGrupo);
                pstmt.executeUpdate();
            }

            // Eliminar miembros
            String sql2 = "DELETE FROM miembros_grupo WHERE grupo_nombre = ?";
            try(PreparedStatement pstmt = conexion.prepareStatement(sql2)){
                pstmt.setString(1, nombreGrupo);
                pstmt.executeUpdate();
            }

            // Eliminar grupo
            String sql3 = "DELETE FROM grupos WHERE nombre = ?";
            try(PreparedStatement pstmt = conexion.prepareStatement(sql3)){
                pstmt.setString(1, nombreGrupo);
                return pstmt.executeUpdate() > 0;
            }
        }catch(SQLException e){
            System.err.println("Error eliminando grupo: " + e.getMessage());
            return false;
        }
    }

    public static void agregarMiembroAGrupo(String nombreGrupo, String username){
        if(conexion == null) return;

        String sql = "INSERT INTO miembros_grupo(grupo_nombre, username) VALUES(?,?) ON CONFLICT DO NOTHING";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, nombreGrupo);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }catch(SQLException e){
            System.err.println("Error agregando miembro al grupo: " + e.getMessage());
        }
    }

    public static void guardarMensaje(String nombreGrupo, String username, String mensaje){
        if(conexion == null) return;

        String sql = "INSERT INTO mensajes_grupo(grupo_nombre, username, mensaje) VALUES(?,?,?)";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, nombreGrupo);
            pstmt.setString(2, username);
            pstmt.setString(3, mensaje);
            pstmt.executeUpdate();
        }catch(SQLException e){
            System.err.println("Error guardando mensaje: " + e.getMessage());
        }
    }

    public static List<String> obtenerMensajesNoLeidos(String nombreGrupo, Timestamp ultimoVisto){
        List<String> mensajes = new ArrayList<>();
        if(conexion == null) return mensajes;

        String sql = "SELECT username, mensaje, fecha_envio FROM mensajes_grupo WHERE grupo_nombre = ? AND fecha_envio > ? ORDER BY fecha_envio ASC LIMIT 50";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, nombreGrupo);
            pstmt.setTimestamp(2, ultimoVisto);

            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                String user = rs.getString("username");
                String msg = rs.getString("mensaje");
                Timestamp fecha = rs.getTimestamp("fecha_envio");
                mensajes.add("[" + fecha + "] " + user + ": " + msg);
            }
        }catch(SQLException e){
            System.err.println("Error obteniendo mensajes no leídos: " + e.getMessage());
        }

        return mensajes;
    }

    public static List<String> obtenerListaGrupos(){
        List<String> grupos = new ArrayList<>();
        if(conexion == null){
            grupos.add("Todos");
            return grupos;
        }

        String sql = "SELECT nombre FROM grupos ORDER BY nombre";
        try(Statement stmt = conexion.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            while(rs.next()){
                grupos.add(rs.getString("nombre"));
            }
        }catch(SQLException e){
            System.err.println("Error obteniendo lista de grupos: " + e.getMessage());
        }

        return grupos;
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
            
            if(rowsAffected > 0){
                inicializarEstadisticas(username);
                return true;
            }
            return false;
        }catch(SQLException e){
            if(e.getSQLState().equals("23505")){
                return false;
            }
            System.err.println("Error al registrar el usuario: "+e.getMessage());
            return false;
        }
    }

    private static void inicializarEstadisticas(String username){
        String sql = "INSERT INTO estadisticas(username, victorias, empates, derrotas, puntos) VALUES(?,0,0,0,0)";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1,username);
            pstmt.executeUpdate();
        }catch(SQLException e){
            System.err.println("Error al inicializar estadisticas: "+e.getMessage());
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

    public static boolean eliminarUsuario(String username){
        if(conexion == null) {
            System.err.println("No hay conexion a la base de datos, no se puede eliminar el usuario: " + username);
            return false;
        }

        try{
            conexion.setAutoCommit(false);
            String sqlEstadisticas = "DELETE FROM estadisticas WHERE username = ?";
            try(PreparedStatement ps = conexion.prepareStatement(sqlEstadisticas)){
                ps.setString(1,username);
                ps.executeUpdate();
            }

            String sqlMiembros = "DELETE FROM miembros_grupo WHERE username = ?";
            try(PreparedStatement ps = conexion.prepareStatement(sqlMiembros)) {
                ps.setString(1, username);
                ps.executeUpdate();
            }

            String sqlMensajes = "DELETE FROM mensajes_grupo WHERE username = ?";
            try(PreparedStatement ps = conexion.prepareStatement(sqlMensajes)) {
                ps.setString(1,username);
                ps.executeUpdate();
            }

            String sqlBloqueos = "DELETE FROM bloqueos WHERE bloqueador = ? OR bloqueado = ?";
            try(PreparedStatement ps = conexion.prepareStatement(sqlBloqueos)) {
                ps.setString(1,username);
                ps.setString(2,username);
                ps.executeUpdate();
            }

            String sqlUsuario = "DELETE FROM usuarios WHERE username = ?";
            try(PreparedStatement ps = conexion.prepareStatement(sqlUsuario)) {
                ps.setString(1,username);
                int filasAfectadas = ps.executeUpdate();

                if(filasAfectadas > 0){
                    conexion.commit();
                    System.out.println("Usuario " + username + " eliminado correctamente");
                    return true;
                }else{
                    conexion.rollback();
                    return false;
            }

            }

        }catch(SQLException e){
            System.err.println("Error al eliminar el usuario: " + e.getMessage());
            try{
                conexion.rollback();
            }catch(SQLException ex){
                System.err.println("Error en el rollback: " + ex.getMessage());
            }
            return false;
        }finally{
            try{
                conexion.setAutoCommit(true);
            }catch(SQLException ex){
                System.err.println("Error al establecer autocommit en true: " + ex.getMessage());
            }
        }
    }

    public static boolean bloquearUsuario(String bloqueador, String bloqueado){
        if(conexion == null) {
            System.err.println("No hay conexion a la base de datos, no se puede bloquear el usuario: " + bloqueado);
            return false;
        }

        if(bloqueador.equals(bloqueado)){
            return false;
        }

        String sql = "INSERT INTO bloqueos(bloqueador, bloqueado) VALUES(?,?) ON CONFLICT DO NOTHING";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1,bloqueador);
            pstmt.setString(2,bloqueado);
            return pstmt.executeUpdate() > 0;
        }catch(SQLException e){
            System.err.println("Error al bloquear el usuario: " + e.getMessage());
            return false;
        }
    }

    public static boolean desbloquearUsuario(String bloqueador, String bloqueado){
        if(conexion == null) {
            System.err.println("No hay conexion a la base de datos, no se puede desbloquear el usuario: " + bloqueado);
            return false;
        }

        String sql = "DELETE FROM bloqueos WHERE bloqueador = ? AND bloqueado = ?";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1,bloqueador);
            pstmt.setString(2,bloqueado);
            return pstmt.executeUpdate() > 0;
        }catch(SQLException e){
            System.err.println("Error al desbloquear el usuario: " + e.getMessage());
            return false;
        }
    }

    public static boolean estaBloqueado(String usuario1, String usuario2){
        if(conexion == null) {
            return false;
        }

        String sql = "SELECT 1 FROM bloqueos WHERE (bloqueador = ? AND bloqueado = ?) OR (bloqueador = ? AND bloqueado = ?)";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1,usuario1);
            pstmt.setString(2,usuario2);
            pstmt.setString(3,usuario2);
            pstmt.setString(4,usuario1);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }catch (SQLException e){
            System.err.println("Error al verificar bloqueo");
            return false;
        }
    }

    public static boolean yoBloquee(String bloqueador, String bloqueado){
        if(conexion == null){
            return false;
        }

        String sql = "SELECT 1 FROM bloqueos WHERE bloqueador = ? AND bloqueado = ?";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }catch(SQLException e){
            System.err.println("Error al verificar si bloqueaste al usuario: " + e.getMessage());
            return false;
        }
    }

    public static List<String> obtenerBloqueados(String username){
        List<String> bloqueados = new ArrayList<>();
        if(conexion == null){
            return bloqueados;
        }

        String sql = "SELECT bloqueado FROM bloqueos WHERE bloqueador = ? ORDER BY fecha_bloqueo DESC";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()){
                bloqueados.add(rs.getString("bloqueado"));
            }
        }catch(SQLException e){
            System.err.println("Error al obtener usuarios bloqueados: " + e.getMessage());
        }

        return bloqueados;
    }

    public static void registrarVictoria(String ganador, String perdedor){
        if(conexion == null) {
            System.out.println("No se pueden registrar estadisticas en modo offline");
            return;
        }

        actualizarEstadistica(ganador, "victorias", 2);
        actualizarEstadistica(perdedor, "derrotas", 0);
    }

    public static void registrarEmpate(String jugador1, String jugador2){
        if(conexion == null) {
            System.out.println("No se pueden registrar estadisticas en modo offline");
            return;
        }
        actualizarEstadistica(jugador1, "empates", 1);
        actualizarEstadistica(jugador2, "empates", 1);
    }

    private static void actualizarEstadistica(String username, String tipo, int puntos){
        String sql = "UPDATE estadisticas SET " + tipo + " = " + tipo + " + 1, puntos = puntos + ? WHERE username = ?";
        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setInt(1, puntos);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();

            if(rowsAffected == 0){
                System.err.println("No se encontraron estadisticas para el usuario: " + username);
            }
        }catch(SQLException e){
            System.err.println("Error actualizando estadistica para " + username + ": " + e.getMessage());
        }
    }

    public static List<String> obtenerRanking(){
        List<String> ranking = new ArrayList<>();
        if(conexion == null){
            ranking.add("Ranking no disponible en modo offline");
            return ranking;
        }

        String sql = "SELECT username, victorias, empates, derrotas, puntos FROM estadisticas ORDER BY puntos DESC, victorias DESC LIMIT 10";

        try(Statement stmt = conexion.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            ranking.add("\n=== RANKING DE JUGADORES ===");
            ranking.add("Pos | Usuario      | V | E | D | Puntos");
            ranking.add("----+--------------+---+---+---+-------");

            int pos = 1;
            boolean hayJugadores = false;
            while(rs.next()){
                hayJugadores = true;
                String usuario = rs.getString("username");
                int victorias = rs.getInt("victorias");
                int empates = rs.getInt("empates");
                int derrotas = rs.getInt("derrotas");
                int puntos = rs.getInt("puntos");

                ranking.add(String.format("%-3d | %-12s | %d | %d | %d | %d",
                        pos++, usuario, victorias, empates, derrotas, puntos));
            }

            if(!hayJugadores){
                ranking.add("No hay jugadores en el ranking todavia");
            }

        }catch(SQLException e){
            System.err.println("Error obteniendo ranking: "+e.getMessage());
            ranking.clear();
            ranking.add("Error al obtener el ranking");
        }

        return ranking;
    }

    public static String obtenerEstadisticasVs(String jugador1, String jugador2){
        if(conexion == null){
            return "Estadisticas no disponibles en modo offline";
        }

        String sql = "SELECT username, victorias, empates, derrotas, puntos FROM estadisticas WHERE username IN (?,?)";

        try(PreparedStatement pstmt = conexion.prepareStatement(sql)){
            pstmt.setString(1, jugador1);
            pstmt.setString(2, jugador2);

            ResultSet rs = pstmt.executeQuery();

            int v1 = 0, e1 = 0, d1 = 0, p1 = 0;
            int v2 = 0, e2 = 0, d2 = 0, p2 = 0;
            boolean encontrado1 = false, encontrado2 = false;

            while(rs.next()){
                String username = rs.getString("username");
                if(username.equals(jugador1)){
                    v1 = rs.getInt("victorias");
                    e1 = rs.getInt("empates");
                    d1 = rs.getInt("derrotas");
                    p1 = rs.getInt("puntos");
                    encontrado1 = true;
                } else if(username.equals(jugador2)){
                    v2 = rs.getInt("victorias");
                    e2 = rs.getInt("empates");
                    d2 = rs.getInt("derrotas");
                    p2 = rs.getInt("puntos");
                    encontrado2 = true;
                }
            }

            if(!encontrado1 && !encontrado2){
                return "Ninguno de los dos jugadores tiene estadisticas registradas";
            } else if(!encontrado1){
                return "El jugador " + jugador1 + " no tiene estadisticas registradas";
            } else if(!encontrado2){
                return "El jugador " + jugador2 + " no tiene estadisticas registradas";
            }

            int total1 = v1 + e1 + d1;
            int total2 = v2 + e2 + d2;

            double porcentajeV1 = total1 > 0 ? (v1 * 100.0 / total1) : 0;
            double porcentajeV2 = total2 > 0 ? (v2 * 100.0 / total2) : 0;

            StringBuilder sb = new StringBuilder();
            sb.append("\n=== ESTADISTICAS: ").append(jugador1).append(" vs ").append(jugador2).append(" ===\n");
            sb.append(String.format("%-12s | V: %d | E: %d | D: %d | Puntos: %d | %% Victorias: %.1f%%\n",
                    jugador1, v1, e1, d1, p1, porcentajeV1));
            sb.append(String.format("%-12s | V: %d | E: %d | D: %d | Puntos: %d | %% Victorias: %.1f%%",
                    jugador2, v2, e2, d2, p2, porcentajeV2));

            return sb.toString();

        }catch(SQLException e){
            System.err.println("Error obteniendo estadisticas vs: "+e.getMessage());
            return "Error al obtener estadisticas";
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
