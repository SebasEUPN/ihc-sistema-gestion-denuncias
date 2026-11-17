package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.ConfiguracionCopiaDTO;
import com.proyecto.denuncias.dto.CopiaDTO;
import com.proyecto.denuncias.dto.CrearCopiaRequest;
import com.proyecto.denuncias.dto.EstadisticasDTO;
import com.proyecto.denuncias.dto.RestauracionDTO;
import com.proyecto.denuncias.dto.RestaurarCopiaRequest;
import com.proyecto.denuncias.model.RegistroActividad;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.AuditoriaService;
import com.proyecto.denuncias.service.CopiaSeguridadService;
import com.proyecto.denuncias.service.EstadisticaService;
import com.proyecto.denuncias.service.ModuloService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Herramientas exclusivas del administrador: auditoría y copias de seguridad.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuditoriaService auditoriaService;
    private final ModuloService moduloService;
    private final EstadisticaService estadisticaService;
    private final CopiaSeguridadService copiaService;

    public AdminController(AuditoriaService auditoriaService, ModuloService moduloService,
                           EstadisticaService estadisticaService, CopiaSeguridadService copiaService) {
        this.copiaService = copiaService;
        this.auditoriaService = auditoriaService;
        this.moduloService = moduloService;
        this.estadisticaService = estadisticaService;
    }

    /** GET /api/admin/estadisticas — tablero con las estadísticas de policías y fiscales. */
    @GetMapping("/estadisticas")
    public EstadisticasDTO estadisticas(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.ADMIN);
        return estadisticaService.calcular();
    }

    /** GET /api/admin/auditoria — registros de actividades de los usuarios. */
    @GetMapping("/auditoria")
    public List<RegistroActividad> auditoria(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.ADMIN);
        return auditoriaService.listar();
    }

    // ------------------------------------------------------------------
    // Copias de seguridad
    // ------------------------------------------------------------------

    /** GET /api/admin/respaldos — historial de copias de seguridad. */
    @GetMapping("/respaldos")
    public List<CopiaDTO> respaldos(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.ADMIN);
        return copiaService.listar();
    }

    /** POST /api/admin/respaldos — crea una copia de seguridad (nombre, tipo y descripción). */
    @PostMapping("/respaldos")
    public CopiaDTO crearRespaldo(@RequestBody CrearCopiaRequest datos, HttpSession sesion) {
        Sesion.Datos admin = Sesion.exigirRol(sesion, Rol.ADMIN);
        return copiaService.crear(admin.id(), datos);
    }

    /** GET /api/admin/respaldos/configuracion — programación de las copias automáticas. */
    @GetMapping("/respaldos/configuracion")
    public ConfiguracionCopiaDTO configuracionRespaldos(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.ADMIN);
        return copiaService.configuracion();
    }

    /** PUT /api/admin/respaldos/configuracion — guarda la programación de las copias automáticas. */
    @PutMapping("/respaldos/configuracion")
    public ConfiguracionCopiaDTO guardarConfiguracionRespaldos(@RequestBody ConfiguracionCopiaDTO datos, HttpSession sesion) {
        Sesion.Datos admin = Sesion.exigirRol(sesion, Rol.ADMIN);
        return copiaService.guardarConfiguracion(admin.id(), datos);
    }

    /** GET /api/admin/respaldos/{id}/archivo — descarga el archivo de una copia. */
    @GetMapping("/respaldos/{id}/archivo")
    public ResponseEntity<Resource> descargarRespaldo(@PathVariable("id") Long id, HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.ADMIN);
        Object[] archivo = copiaService.archivo(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename((String) archivo[0], StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body((Resource) archivo[1]);
    }

    /** POST /api/admin/respaldos/{id}/restaurar — restaura una copia (pide la contraseña del administrador). */
    @PostMapping("/respaldos/{id}/restaurar")
    public RestauracionDTO restaurarRespaldo(@PathVariable("id") Long id, @RequestBody RestaurarCopiaRequest datos,
                                             HttpSession sesion) {
        Sesion.Datos admin = Sesion.exigirRol(sesion, Rol.ADMIN);
        return copiaService.restaurar(id, admin.id(), datos.password());
    }
}
