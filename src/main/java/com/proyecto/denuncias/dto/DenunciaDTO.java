package com.proyecto.denuncias.dto;

import com.proyecto.denuncias.model.Ciudadano;
import com.proyecto.denuncias.model.Denuncia;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Datos de una denuncia que se envían al frontend.
 * El campo "estado" depende del perfil que consulta: el fiscal recibe el
 * estado fiscal y el policía (o el administrador) el estado policial.
 */
public record DenunciaDTO(
        String codigo,
        String ciudadano,              // nombre corto para las tablas
        String ciudadanoCompleto,
        String dniCiudadano,
        String correoCiudadano,
        String telefonoCiudadano,
        String tipoDelito,
        String estado,
        String fecha,                  // fecha de registro en formato dd/MM/yyyy
        LocalDateTime fechaRegistro,
        LocalDateTime ultimaActualizacion,
        String lugarHechos,
        String descripcion,
        String despachoFiscal,
        String fiscalResponsable,
        String carpetaFiscal,
        List<SeguimientoDTO> historial // solo se llena al consultar el detalle
) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Crea el DTO a partir de la entidad.
     * @param vistaFiscal true si quien consulta es un fiscal
     * @param historial   registros de seguimiento (lista vacía en los listados)
     */
    public static DenunciaDTO de(Denuncia d, boolean vistaFiscal, List<SeguimientoDTO> historial) {
        Ciudadano c = d.getCiudadano();
        return new DenunciaDTO(
                d.getCodigo(), c.getNombreCorto(), c.getNombreCompleto(), c.getDni(), c.getCorreo(),
                c.getTelefono(), d.getTipoDelito(),
                vistaFiscal ? d.getEstadoFiscal() : d.getEstadoPolicial(),
                d.getFechaRegistro().format(FORMATO_FECHA), d.getFechaRegistro(),
                d.getUltimaActualizacion(), d.getLugarHechos(), d.getDescripcion(),
                d.getDespachoFiscal(), d.getFiscalResponsable(), d.getCarpetaFiscal(), historial);
    }
}
