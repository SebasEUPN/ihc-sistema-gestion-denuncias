package com.proyecto.denuncias.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Expediente completo de una denuncia: lo que registró la PNP y lo que
 * informa la Fiscalía (carpeta, etapa de la investigación, disposiciones,
 * actuaciones, documentos e historial). Se muestra en las ventanas de
 * detalle de actuaciones, disposiciones, informes y seguimiento de Fiscalía.
 */
public record ExpedienteDTO(
        String codigo,
        String tipoDelito,
        String descripcion,
        String lugarHechos,
        String distrito,
        String provincia,
        LocalDateTime fechaRegistro,
        LocalDateTime ultimaActualizacion,
        String estadoPolicial,
        String estadoFiscal,
        String carpetaFiscal,
        String despachoFiscal,
        String fiscalResponsable,
        Persona ciudadano,
        Funcionario responsablePolicial,      // null si no tiene responsable asignado
        Remision remision,                    // null si la PNP no registró una remisión
        boolean requerimiento,                // true si la Fiscalía pidió información adicional
        List<Etapa> etapas,                   // etapas de la investigación fiscal
        int etapaActual,                      // posición (desde 0) de la etapa en curso
        int avance,                           // porcentaje de avance de la investigación fiscal
        List<DisposicionItem> disposiciones,
        List<ActuacionItem> actuaciones,
        List<DocumentoDTO> documentos,
        List<SeguimientoDTO> historial
) {

    public record Persona(String nombre, String dni, String telefono, String correo) {
    }

    public record Funcionario(String codigo, String nombre, String rango) {
    }

    public record Remision(LocalDate fechaRemision, LocalDate ultimaRespuesta, String estado) {
    }

    /** Etapa de la investigación. estado: COMPLETADO, EN_CURSO o PENDIENTE. */
    public record Etapa(String titulo, String estado, LocalDate fecha, String detalle) {
    }

    public record DisposicionItem(String numero, String tipo, LocalDate fecha, String fiscalResponsable, String estado) {
    }

    public record ActuacionItem(String codigo, String perfil, String descripcion, LocalDate fecha, String estado) {
    }
}
