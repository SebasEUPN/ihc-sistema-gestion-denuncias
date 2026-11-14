package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.EstadisticasDTO;
import com.proyecto.denuncias.model.CopiaSeguridad;
import com.proyecto.denuncias.model.RegistroActividad;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.AuditoriaService;
import com.proyecto.denuncias.service.EstadisticaService;
import com.proyecto.denuncias.service.ModuloService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public AdminController(AuditoriaService auditoriaService, ModuloService moduloService,
                           EstadisticaService estadisticaService) {
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

    /** GET /api/admin/respaldos — historial de copias de seguridad. */
    @GetMapping("/respaldos")
    public List<CopiaSeguridad> respaldos(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.ADMIN);
        return moduloService.copias();
    }

    /** POST /api/admin/respaldos — registra una copia de seguridad demostrativa. */
    @PostMapping("/respaldos")
    public CopiaSeguridad generarRespaldo(HttpSession sesion) {
        Sesion.Datos admin = Sesion.exigirRol(sesion, Rol.ADMIN);
        return moduloService.generarCopia(admin.codigo());
    }
}
