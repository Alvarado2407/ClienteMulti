CREATE DATABASE chat_db
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

\c chat_db;


CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bloqueos (
    id SERIAL PRIMARY KEY,
    bloqueador VARCHAR(50) NOT NULL,
    bloqueado VARCHAR(50) NOT NULL,
    fecha_bloqueo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(bloqueador, bloqueado),
    FOREIGN KEY (bloqueador) REFERENCES usuarios(username) ON DELETE CASCADE,
    FOREIGN KEY (bloqueado) REFERENCES usuarios(username) ON DELETE CASCADE
);


\dt