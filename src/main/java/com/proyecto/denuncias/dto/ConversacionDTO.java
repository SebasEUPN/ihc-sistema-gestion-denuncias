package com.proyecto.denuncias.dto;

import java.util.List;

/** Conversación del chat vista por el usuario que inició sesión. */
public record ConversacionDTO(
        Long id,
        String nombre,          // nombre de la otra persona de la conversación
        String telefono,        // teléfono del ciudadano (vacío entre funcionarios)
        String referencia,      // denuncia o asunto relacionado
        boolean enLinea,        // true si el otro funcionario inició sesión hace poco
        long noLeidos,          // mensajes recibidos que aún no se han leído
        List<MensajeDTO> mensajes
) {
}
