package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.CambiarPasswordRequest;
import com.proyecto.denuncias.dto.LoginRequest;
import com.proyecto.denuncias.dto.RecuperarRequest;
import com.proyecto.denuncias.dto.UsuarioDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.security.UsuariosActivos;
import com.proyecto.denuncias.service.AuthService;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador de autenticación. Sus rutas (/api/auth/**) son las únicas
 * que pueden usarse sin haber iniciado sesión.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final UsuariosActivos usuariosActivos;

    public AuthController(AuthService authService, UsuarioService usuarioService, UsuariosActivos usuariosActivos) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.usuariosActivos = usuariosActivos;
    }

    /** POST /api/auth/login — valida las credenciales y crea la sesión. */
    @PostMapping("/login")
    public UsuarioDTO login(@RequestBody LoginRequest datos, HttpServletRequest request) {
        Usuario usuario = authService.iniciarSesion(datos);
        // Se cambia el identificador de sesión al autenticarse (evita el ataque de "fijación de sesión")
        HttpSession sesion = request.getSession();
        request.changeSessionId();
        sesion.removeAttribute(Sesion.ATRIBUTO_RECUPERACION);
        Sesion.iniciar(sesion, usuario);
        return UsuarioDTO.de(usuario);
    }

    /**
     * GET /api/auth/sesion — el frontend la consulta al cargar la página para saber
     * si ya hay una sesión abierta. Responde 204 (sin contenido) cuando no la hay.
     */
    @GetMapping("/sesion")
    public ResponseEntity<UsuarioDTO> sesion(HttpSession sesion) {
        Sesion.Datos datos = Sesion.buscar(sesion);
        if (datos == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(UsuarioDTO.de(usuarioService.obtener(datos.id())));
    }

    /** POST /api/auth/logout — destruye la sesión del servidor. */
    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession sesion) {
        // El usuario deja de contarse entre los que están usando el sistema
        Sesion.Datos usuario = Sesion.buscar(sesion);
        if (usuario != null) {
            usuariosActivos.retirar(usuario.id());
        }
        sesion.invalidate();
        return Map.of("mensaje", "Sesión cerrada.");
    }

    /**
     * POST /api/auth/recuperar — primer paso para recuperar la contraseña.
     * Si código, perfil y DNI coinciden, se recuerda en la sesión qué usuario
     * quedó verificado para permitirle definir una nueva contraseña.
     */
    @PostMapping("/recuperar")
    public Map<String, String> recuperar(@RequestBody RecuperarRequest datos, HttpSession sesion) {
        Long idUsuario = authService.verificarIdentidad(datos);
        sesion.setAttribute(Sesion.ATRIBUTO_RECUPERACION, idUsuario);
        return Map.of("mensaje", "Identidad verificada. Define tu nueva contraseña.");
    }

    /**
     * POST /api/auth/cambiar-password — guarda la nueva contraseña.
     * Funciona en dos casos: usuario con sesión iniciada (desde Configuración)
     * o usuario que acaba de verificar su identidad en la recuperación.
     */
    @PostMapping("/cambiar-password")
    public Map<String, String> cambiarPassword(@RequestBody CambiarPasswordRequest datos, HttpSession sesion) {
        Sesion.Datos autenticado = Sesion.buscar(sesion);
        Long idRecuperacion = (Long) sesion.getAttribute(Sesion.ATRIBUTO_RECUPERACION);

        Long idUsuario = autenticado != null ? autenticado.id() : idRecuperacion;
        if (idUsuario == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Primero verifica tu identidad para cambiar la contraseña.");
        }
        authService.cambiarPassword(idUsuario, datos.nuevaPassword());
        // Tras el cambio se cierra la sesión: el usuario debe ingresar con la nueva contraseña
        sesion.invalidate();
        return Map.of("mensaje", "Contraseña actualizada.");
    }
}
