package com.proyecto.denuncias.dto;

import java.time.LocalDate;

/** Datos del formulario "Registrar funcionario" que completa el administrador. */
public record RegistroFuncionarioRequest(
        String rol,                // "policia" o "fiscal"
        String nombres,
        String apellidos,
        String dni,
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        String institucion,        // "PNP" o "Fiscalía"
        String dependencia,
        String codigo,             // código institucional de acceso
        String password,           // contraseña temporal
        boolean habilitado
) {
}
