package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.LoginRequest;
import com.proyecto.denuncias.dto.RecuperarRequest;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.repository.UsuarioRepository;
import com.proyecto.denuncias.util.Validar;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lógica de autenticación: inicio de sesión, recuperación y cambio de contraseña.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Verifica las credenciales y devuelve el usuario autenticado.
     * Si el código no existe o la contraseña es incorrecta se responde con el
     * MISMO mensaje, para no revelar qué códigos están registrados.
     */
    @Transactional
    public Usuario iniciarSesion(LoginRequest datos) {
        String codigo = Validar.texto(datos.codigo(), "Código institucional", 20);
        String password = datos.password() == null ? "" : datos.password();
        Rol rolElegido = Rol.desde(datos.rol());

        // 1. Se busca el usuario por su código institucional
        Usuario usuario = usuarioRepository.findByCodigoIgnoreCase(codigo).orElse(null);

        // 2. Se compara la contraseña escrita con el hash BCrypt guardado en la base de datos
        if (usuario == null || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Código institucional o contraseña incorrectos.");
        }
        // 3. El perfil elegido en el formulario debe coincidir con el perfil real de la cuenta
        if (usuario.getRol() != rolElegido) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "El perfil seleccionado no corresponde a tu cuenta.");
        }
        // 4. Las cuentas deshabilitadas por el administrador no pueden ingresar
        if (!usuario.isHabilitado()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta está deshabilitada. Comunícate con el administrador.");
        }

        // 5. Se registra la fecha del acceso y la actividad en la auditoría
        usuario.setUltimoAcceso(LocalDateTime.now());
        auditoriaService.registrar(usuario.getCodigo(), "Inicio de sesión (" + usuario.getRol().getEtiqueta() + ")", "Acceso");
        return usuario;
    }

    /**
     * Primer paso de la recuperación de contraseña: se verifica la identidad
     * comparando código, perfil y DNI. Devuelve el id del usuario verificado.
     */
    @Transactional(readOnly = true)
    public Long verificarIdentidad(RecuperarRequest datos) {
        String codigo = Validar.texto(datos.codigo(), "Código institucional", 20);
        String dni = Validar.texto(datos.dni(), "DNI", 8);
        Rol rol = Rol.desde(datos.rol());

        Usuario usuario = usuarioRepository.findByCodigoIgnoreCase(codigo).orElse(null);
        if (usuario == null || usuario.getRol() != rol || !usuario.getDni().equals(dni) || !usuario.isHabilitado()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los datos no coinciden con una cuenta activa.");
        }
        return usuario.getId();
    }

    /** Guarda la nueva contraseña cifrada con BCrypt. */
    @Transactional
    public void cambiarPassword(Long idUsuario, String nuevaPassword) {
        Validar.password(nuevaPassword);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        auditoriaService.registrar(usuario.getCodigo(), "Cambio de contraseña", "Acceso");
    }
}
