-- Crear la base de datos
CREATE DATABASE clienteMulti
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

-- Conectar a la base recién creada
\c clienteMulti;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de bloqueos
CREATE TABLE IF NOT EXISTS bloqueos (
    id SERIAL PRIMARY KEY,
    bloqueador VARCHAR(50) NOT NULL,
    bloqueado VARCHAR(50) NOT NULL,
    fecha_bloqueo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(bloqueador, bloqueado),
    FOREIGN KEY (bloqueador) REFERENCES usuarios(username) ON DELETE CASCADE,
    FOREIGN KEY (bloqueado) REFERENCES usuarios(username) ON DELETE CASCADE
);

-- Mostrar las tablas creadas
\dt
