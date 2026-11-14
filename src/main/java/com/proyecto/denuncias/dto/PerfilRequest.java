package com.proyecto.denuncias.dto;

/** Datos de contacto que el funcionario puede editar en "Mi perfil". */
public record PerfilRequest(String telefono, String correo, String direccion, String contactoEmergencia) {
}
