package com.proyecto.denuncias.dto;

import com.proyecto.denuncias.model.Usuario;

import java.util.List;

/**
 * Datos de un funcionario que se envían al frontend.
 * Se usa un DTO (objeto de transferencia) en lugar de la entidad para que
 * NUNCA salga del servidor la contraseña cifrada.
 */
public record UsuarioDTO(
        String codigo,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String dni,
        String institucion,
        String rol,                 // "admin", "policia" o "fiscal"
        String perfil,              // texto visible: "Funcionario policial"
        String rango,
        String dependencia,
        String telefono,
        String correo,
        String direccion,
        String contactoEmergencia,
        boolean habilitado,
        boolean tieneFoto,
        List<String> permisos       // módulos habilitados
) {

    /** Crea el DTO a partir de la entidad Usuario. */
    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(
                u.getCodigo(), u.getNombres(), u.getApellidos(), u.getNombreCompleto(), u.getDni(),
                u.getInstitucion(), u.getRol().getClave(), u.getRol().getEtiqueta(), u.getRango(),
                u.getDependencia(), u.getTelefono(), u.getCorreo(), u.getDireccion(),
                u.getContactoEmergencia(), u.isHabilitado(), u.getFoto() != null,
                u.getPermisos().stream().sorted().toList());
    }
}
