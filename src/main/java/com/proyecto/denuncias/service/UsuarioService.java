package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.PerfilRequest;
import com.proyecto.denuncias.dto.RegistroFuncionarioRequest;
import com.proyecto.denuncias.dto.UsuarioDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.repository.UsuarioRepository;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.util.Validar;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lógica de negocio de los usuarios: listado, registro, habilitación,
 * permisos por módulo y edición del perfil propio.
 */
@Service
public class UsuarioService {

    /** Módulos que el administrador puede habilitar o deshabilitar a un funcionario. */
    public static final Set<String> MODULOS = Set.of("denuncias", "tracking", "documents", "reports", "chat", "schedule");

    /** Tipos de imagen aceptados como foto de perfil. */
    private static final Set<String> TIPOS_FOTO = Set.of("image/png", "image/jpeg", "image/webp");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final ArchivoService archivoService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService, ArchivoService archivoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.archivoService = archivoService;
    }

    /** Busca un usuario por id; si ya no existe la sesión deja de ser válida. */
    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida."));
    }

    /** Busca un usuario por su código institucional. */
    private Usuario obtenerPorCodigo(String codigo) {
        return usuarioRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe un usuario con el código " + codigo + "."));
    }

    /** Lista todos los usuarios (pantallas "Funcionarios" y "Usuarios y perfiles"). */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAllByOrderByIdAsc().stream().map(UsuarioDTO::de).toList();
    }

    /**
     * Verifica que el usuario de la sesión tenga habilitado un módulo.
     * El administrador siempre tiene acceso a todo.
     */
    @Transactional(readOnly = true)
    public void exigirPermiso(Sesion.Datos sesion, String modulo) {
        if (sesion.rol() == Rol.ADMIN) {
            return;
        }
        Usuario usuario = obtener(sesion.id());
        if (!usuario.isHabilitado()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta está deshabilitada.");
        }
        if (!usuario.getPermisos().contains(modulo)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes habilitado este módulo. Solicítalo al administrador.");
        }
    }

    /** Registra un nuevo funcionario policial o fiscal (solo el administrador). */
    @Transactional
    public UsuarioDTO registrar(RegistroFuncionarioRequest datos, String codigoAdmin) {
        // --- Validación de cada campo del formulario ---
        Rol rol = Rol.desde(datos.rol());
        if (rol == Rol.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se pueden registrar funcionarios policiales o fiscales.");
        }
        String nombres = Validar.texto(datos.nombres(), "Nombres", 80);
        String apellidos = Validar.texto(datos.apellidos(), "Apellidos", 80);
        String dni = Validar.formato(datos.dni(), "DNI", "\\d{8}", "debe tener 8 dígitos");
        String telefono = Validar.formato(datos.telefono(), "Teléfono", "\\d{9}", "debe tener 9 dígitos");
        String correo = Validar.formato(datos.correo(), "Correo institucional", "[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "no es un correo válido");
        String dependencia = Validar.texto(datos.dependencia(), "Dependencia / despacho", 120);
        String codigo = Validar.texto(datos.codigo(), "Código institucional", 20).toUpperCase();
        String password = Validar.password(datos.password());
        if (datos.fechaNacimiento() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento es obligatoria.");
        }

        // --- El perfil debe corresponder con la institución ---
        String institucionEsperada = rol == Rol.POLICIA ? "PNP" : "Fiscalía";
        if (!institucionEsperada.equals(datos.institucion())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El perfil seleccionado debe corresponder con la institución indicada.");
        }

        // --- No se permiten códigos ni DNI repetidos ---
        if (usuarioRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese código institucional ya está registrado.");
        }
        if (usuarioRepository.existsByDni(dni)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un funcionario registrado con ese DNI.");
        }

        // --- Se crea la entidad y se guarda ---
        Usuario usuario = new Usuario();
        usuario.setCodigo(codigo);
        usuario.setPasswordHash(passwordEncoder.encode(password)); // se guarda cifrada
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setDni(dni);
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setTelefono(telefono);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setDependencia(dependencia);
        usuario.setHabilitado(datos.habilitado());
        usuario.setPermisos(new HashSet<>(MODULOS)); // por defecto recibe todos los módulos
        usuarioRepository.save(usuario);

        auditoriaService.registrar(codigoAdmin, "Registró al funcionario " + codigo, "Usuarios");
        return UsuarioDTO.de(usuario);
    }

    /** Habilita o deshabilita una cuenta (solo el administrador). */
    @Transactional
    public UsuarioDTO alternarEstado(String codigo, String codigoAdmin) {
        Usuario usuario = obtenerPorCodigo(codigo);
        // Un administrador no puede deshabilitar su propia cuenta (se quedaría sin acceso)
        if (usuario.getCodigo().equalsIgnoreCase(codigoAdmin)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes deshabilitar tu propia cuenta.");
        }
        // Siempre debe quedar al menos un administrador habilitado
        if (usuario.getRol() == Rol.ADMIN && usuario.isHabilitado()
                && usuarioRepository.countByRolAndHabilitadoTrue(Rol.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe quedar al menos un administrador habilitado.");
        }
        usuario.setHabilitado(!usuario.isHabilitado());
        auditoriaService.registrar(codigoAdmin,
                (usuario.isHabilitado() ? "Habilitó" : "Deshabilitó") + " la cuenta " + usuario.getCodigo(), "Usuarios");
        return UsuarioDTO.de(usuario);
    }

    /** Reemplaza los módulos habilitados de un funcionario (solo el administrador). */
    @Transactional
    public UsuarioDTO guardarPermisos(String codigo, List<String> modulos, String codigoAdmin) {
        Usuario usuario = obtenerPorCodigo(codigo);
        Set<String> nuevos = new HashSet<>(modulos == null ? List.of() : modulos);
        // Solo se aceptan módulos que realmente existen en el sistema
        if (!MODULOS.containsAll(nuevos)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La lista contiene un módulo que no existe.");
        }
        usuario.getPermisos().clear();
        usuario.getPermisos().addAll(nuevos);
        auditoriaService.registrar(codigoAdmin, "Actualizó los permisos de " + usuario.getCodigo(), "Usuarios");
        return UsuarioDTO.de(usuario);
    }

    /** Actualiza los datos de contacto del propio funcionario ("Mi perfil"). */
    @Transactional
    public UsuarioDTO actualizarContacto(Long idUsuario, PerfilRequest datos) {
        Usuario usuario = obtener(idUsuario);
        usuario.setTelefono(Validar.formato(datos.telefono(), "Número de celular", "[\\d +]{6,15}", "solo admite dígitos y espacios (6 a 15 caracteres)"));
        usuario.setCorreo(Validar.formato(datos.correo(), "Correo de contacto", "[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "no es un correo válido"));
        usuario.setDireccion(Validar.texto(datos.direccion(), "Dirección", 150));
        usuario.setContactoEmergencia(Validar.opcional(datos.contactoEmergencia(), "Contacto de emergencia", 15));
        auditoriaService.registrar(usuario.getCodigo(), "Actualizó sus datos de contacto", "Perfil");
        return UsuarioDTO.de(usuario);
    }

    /** Guarda la foto de perfil del funcionario (PNG, JPG o WEBP de hasta 5 MB). */
    @Transactional
    public UsuarioDTO guardarFoto(Long idUsuario, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona una fotografía.");
        }
        if (!TIPOS_FOTO.contains(foto.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fotografía debe ser PNG, JPG o WEBP.");
        }
        if (foto.getSize() > 5L * 1024 * 1024) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fotografía no puede superar los 5 MB.");
        }
        Usuario usuario = obtener(idUsuario);
        String anterior = usuario.getFoto();
        usuario.setFoto(archivoService.guardar(foto));
        usuario.setFotoTipo(foto.getContentType());
        archivoService.eliminar(anterior); // se borra la foto anterior para no acumular archivos
        return UsuarioDTO.de(usuario);
    }
}
