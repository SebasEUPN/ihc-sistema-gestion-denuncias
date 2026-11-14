package com.proyecto.denuncias.service;

import com.proyecto.denuncias.exception.ApiException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Guarda y recupera archivos (adjuntos del chat, fotos de perfil y documentos).
 *
 * Versión en la nube: el contenido se guarda en la tabla archivo_almacenado de
 * la base de datos, porque en Render el disco es temporal (se borra en cada
 * despliegue) y el portal ciudadano corre en otro servidor y también necesita
 * leer los adjuntos del chat. Las demás tablas siguen guardando solo el nombre
 * (UUID) del archivo, igual que cuando se usaba la carpeta "uploads".
 */
@Service
public class ArchivoService {

    private final JdbcTemplate jdbc;

    public ArchivoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Guarda el archivo con un nombre aleatorio (UUID) y devuelve ese nombre.
     * No se usa el nombre original para evitar choques y rutas maliciosas.
     */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se recibió ningún archivo.");
        }
        try {
            return guardarBytes(archivo.getBytes());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo.");
        }
    }

    /** Guarda contenido generado por el sistema (por ejemplo un PDF) y devuelve el nombre asignado. */
    public String guardarBytes(byte[] contenido) {
        String nombre = UUID.randomUUID().toString();
        try {
            jdbc.update("INSERT INTO archivo_almacenado (nombre, contenido) VALUES (?, ?)", nombre, contenido);
            return nombre;
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo.");
        }
    }

    /** Devuelve el archivo guardado para enviarlo al navegador. */
    public Resource cargar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El archivo ya no está disponible.");
        }
        List<byte[]> filas = jdbc.query("SELECT contenido FROM archivo_almacenado WHERE nombre = ?",
                (rs, i) -> rs.getBytes(1), nombre);
        if (filas.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El archivo ya no está disponible.");
        }
        return new ByteArrayResource(filas.get(0));
    }

    /** Elimina un archivo (por ejemplo, la foto anterior al subir una nueva). */
    public void eliminar(String nombre) {
        if (nombre == null) {
            return;
        }
        try {
            jdbc.update("DELETE FROM archivo_almacenado WHERE nombre = ?", nombre);
        } catch (RuntimeException e) {
            // Si no se puede borrar no se interrumpe la operación principal
        }
    }
}
