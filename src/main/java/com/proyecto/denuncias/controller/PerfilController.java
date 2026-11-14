package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.PerfilRequest;
import com.proyecto.denuncias.dto.UsuarioDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.ArchivoService;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * "Mi perfil": cada funcionario consulta y edita únicamente SUS propios datos
 * (el usuario se toma de la sesión, nunca de un parámetro enviado por el navegador).
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;
    private final ArchivoService archivoService;

    public PerfilController(UsuarioService usuarioService, ArchivoService archivoService) {
        this.usuarioService = usuarioService;
        this.archivoService = archivoService;
    }

    /** GET /api/perfil — datos del funcionario que inició sesión. */
    @GetMapping
    public UsuarioDTO obtener(HttpSession sesion) {
        return UsuarioDTO.de(usuarioService.obtener(Sesion.actual(sesion).id()));
    }

    /** PUT /api/perfil — actualiza teléfono, correo, dirección y contacto de emergencia. */
    @PutMapping
    public UsuarioDTO actualizar(@RequestBody PerfilRequest datos, HttpSession sesion) {
        return usuarioService.actualizarContacto(Sesion.actual(sesion).id(), datos);
    }

    /** POST /api/perfil/foto — sube una nueva foto de perfil (formulario multipart, campo "foto"). */
    @PostMapping("/foto")
    public UsuarioDTO subirFoto(@RequestParam("foto") MultipartFile foto, HttpSession sesion) {
        return usuarioService.guardarFoto(Sesion.actual(sesion).id(), foto);
    }

    /** GET /api/perfil/foto — devuelve la imagen de la foto de perfil. */
    @GetMapping("/foto")
    public ResponseEntity<Resource> verFoto(HttpSession sesion) {
        Usuario usuario = usuarioService.obtener(Sesion.actual(sesion).id());
        if (usuario.getFoto() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Aún no tienes foto de perfil.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(usuario.getFotoTipo()))
                .body(archivoService.cargar(usuario.getFoto()));
    }
}
