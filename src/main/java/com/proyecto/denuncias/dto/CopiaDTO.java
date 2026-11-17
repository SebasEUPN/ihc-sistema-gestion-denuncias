package com.proyecto.denuncias.dto;

import com.proyecto.denuncias.model.CopiaSeguridad;

import java.time.LocalDateTime;

/** Copia de seguridad tal como se muestra en el historial del administrador. */
public record CopiaDTO(
        Long id,
        String nombre,
        String tipo,
        String tipoNombre,
        String descripcion,
        String origen,
        LocalDateTime fechaHora,
        long tamanoBytes,
        int registros,
        String estado,
        String administrador,
        boolean disponible) {   // true = tiene archivo: se puede descargar y restaurar

    public static CopiaDTO de(CopiaSeguridad c, String tipoNombre) {
        return new CopiaDTO(c.getId(), c.getNombre(), c.getTipo(), tipoNombre, c.getDescripcion(), c.getOrigen(),
                c.getFechaHora(), c.getTamanoBytes(), c.getRegistros(), c.getEstado(),
                c.getAdmin().getCodigo() + " · " + c.getAdmin().getNombreCompleto(),
                CopiaSeguridad.COMPLETADA.equals(c.getEstado()) && c.getArchivo() != null);
    }
}
