package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.PermisosRequest;
import com.proyecto.denuncias.dto.RegistroFuncionarioRequest;
import com.proyecto.denuncias.dto.UsuarioDTO;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administración de usuarios. Todas las rutas exigen el perfil ADMIN.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** GET /api/usuarios — lista de funcionarios y administradores. */
    @GetMapping
    public List<UsuarioDTO> listar(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.ADMIN);
        return usuarioService.listar();
    }

    /** POST /api/usuarios — registra un funcionario policial o fiscal. */
    @PostMapping
    public UsuarioDTO registrar(@RequestBody RegistroFuncionarioRequest datos, HttpSession sesion) {
        Sesion.Datos admin = Sesion.exigirRol(sesion, Rol.ADMIN);
        return usuarioService.registrar(datos, admin.codigo());
    }

    /** PATCH /api/usuarios/{codigo}/estado — habilita o deshabilita la cuenta. */
    @PatchMapping("/{codigo}/estado")
    public UsuarioDTO alternarEstado(@PathVariable("codigo") String codigo, HttpSession sesion) {
        Sesion.Datos admin = Sesion.exigirRol(sesion, Rol.ADMIN);
        return usuarioService.alternarEstado(codigo, admin.codigo());
    }

    /** PUT /api/usuarios/{codigo}/permisos — reemplaza los módulos habilitados. */
    @PutMapping("/{codigo}/permisos")
    public UsuarioDTO guardarPermisos(@PathVariable("codigo") String codigo, @RequestBody PermisosRequest datos, HttpSession sesion) {
        Sesion.Datos admin = Sesion.exigirRol(sesion, Rol.ADMIN);
        return usuarioService.guardarPermisos(codigo, datos.modulos(), admin.codigo());
    }
}
