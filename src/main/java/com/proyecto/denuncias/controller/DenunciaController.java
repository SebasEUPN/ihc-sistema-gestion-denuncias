package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.ActualizarDenunciaRequest;
import com.proyecto.denuncias.dto.DenunciaDTO;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.DenunciaService;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Denuncias (para el fiscal se muestran como "carpetas fiscales").
 */
@RestController
@RequestMapping("/api/denuncias")
public class DenunciaController {

    private final DenunciaService denunciaService;
    private final UsuarioService usuarioService;

    public DenunciaController(DenunciaService denunciaService, UsuarioService usuarioService) {
        this.denunciaService = denunciaService;
        this.usuarioService = usuarioService;
    }

    /** GET /api/denuncias — listado completo según el perfil de la sesión. */
    @GetMapping
    public List<DenunciaDTO> listar(HttpSession sesion) {
        Sesion.Datos usuario = Sesion.actual(sesion);
        usuarioService.exigirPermiso(usuario, "denuncias");
        return denunciaService.listar(usuario.rol());
    }

    /**
     * GET /api/denuncias/reporte?desde=2026-10-30&hasta=2026-11-06
     * Denuncias registradas dentro del periodo (módulo de reportes).
     */
    @GetMapping("/reporte")
    public List<DenunciaDTO> reporte(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession sesion) {
        Sesion.Datos usuario = Sesion.actual(sesion);
        usuarioService.exigirPermiso(usuario, "reports");
        return denunciaService.reporte(desde, hasta, usuario);
    }

    /** GET /api/denuncias/{codigo} — detalle con historial de seguimiento. */
    @GetMapping("/{codigo}")
    public DenunciaDTO obtener(@PathVariable("codigo") String codigo, HttpSession sesion) {
        Sesion.Datos usuario = Sesion.actual(sesion);
        usuarioService.exigirPermiso(usuario, "denuncias");
        return denunciaService.obtener(codigo, usuario.rol());
    }

    /** PUT /api/denuncias/{codigo} — cambia el estado y agrega una observación (policía o fiscal). */
    @PutMapping("/{codigo}")
    public DenunciaDTO actualizar(@PathVariable("codigo") String codigo, @RequestBody ActualizarDenunciaRequest datos, HttpSession sesion) {
        // El administrador solo consulta: no puede modificar expedientes
        Sesion.Datos usuario = Sesion.exigirRol(sesion, Rol.POLICIA, Rol.FISCAL);
        usuarioService.exigirPermiso(usuario, "denuncias");
        return denunciaService.actualizar(codigo, datos, usuario);
    }
}
