package com.proyecto.denuncias.dto;

import com.proyecto.denuncias.model.Denuncia;
import com.proyecto.denuncias.model.Documento;
import com.proyecto.denuncias.model.Usuario;

import java.time.LocalDate;

/**
 * Documento o evidencia tal como lo necesita la pantalla "Documentos y evidencias".
 * No incluye el nombre interno con el que el archivo se guardó en disco.
 */
public record DocumentoDTO(
        String codigo,
        String nombre,
        String descripcion,
        String codigoDenuncia,
        String tipoDelito,           // de la denuncia relacionada
        String ciudadano,            // de la denuncia relacionada
        String responsable,          // código del funcionario que lo registró
        String responsableNombre,
        LocalDate fecha,
        String estado,
        boolean tieneArchivo,
        String archivoNombre,
        String archivoTipo,
        long archivoTamano,
        String categoria             // pdf, imagen, video, audio u otro (decide cómo se previsualiza)
) {

    public static DocumentoDTO de(Documento d, Denuncia denuncia, Usuario responsable) {
        return new DocumentoDTO(d.getCodigo(), d.getNombre(), d.getDescripcion(), d.getCodigoDenuncia(),
                denuncia == null ? null : denuncia.getTipoDelito(),
                denuncia == null ? null : denuncia.getCiudadano().getNombreCompleto(),
                d.getCodigoResponsable(), responsable == null ? null : responsable.getNombreCompleto(),
                d.getFecha(), d.getEstado(), d.getArchivo() != null, d.getArchivoNombre(), d.getArchivoTipo(),
                d.getArchivoTamano() == null ? 0 : d.getArchivoTamano(), categoria(d.getArchivoTipo()));
    }

    /** Clasifica el archivo según su tipo para saber cómo mostrarlo. */
    public static String categoria(String tipo) {
        if (tipo == null) return "ninguno";
        if (tipo.equals("application/pdf")) return "pdf";
        if (tipo.startsWith("image/")) return "imagen";
        if (tipo.startsWith("video/")) return "video";
        if (tipo.startsWith("audio/")) return "audio";
        return "otro";
    }
}
