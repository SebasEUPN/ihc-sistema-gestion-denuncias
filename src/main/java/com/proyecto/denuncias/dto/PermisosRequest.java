package com.proyecto.denuncias.dto;

import java.util.List;

/** Lista de módulos que el administrador habilita para un funcionario. */
public record PermisosRequest(List<String> modulos) {
}
