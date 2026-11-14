package com.proyecto.denuncias.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Captura las excepciones de todos los controladores y las convierte en
 * una respuesta JSON uniforme: { "mensaje": "texto para el usuario" }.
 * El frontend muestra ese texto en la notificación (toast).
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** Errores controlados por el sistema (validaciones, permisos, no encontrado...). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> manejarApi(ApiException e) {
        return ResponseEntity.status(e.getEstado()).body(Map.of("mensaje", e.getMessage()));
    }

    /** El archivo subido supera el tamaño máximo configurado en application.properties. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> manejarArchivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("mensaje", "El archivo supera el tamaño máximo permitido (20 MB)."));
    }

    /** El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensaje", "Los datos enviados no tienen un formato válido."));
    }
}
