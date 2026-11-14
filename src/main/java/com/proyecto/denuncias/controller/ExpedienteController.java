package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.DisposicionDTO;
import com.proyecto.denuncias.dto.ExpedienteDTO;
import com.proyecto.denuncias.dto.RemisionDTO;
import com.proyecto.denuncias.dto.SeguimientoFiscalDTO;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.ExpedienteService;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Información fiscal de las denuncias: expediente completo, seguimiento de
 * la Fiscalía, informes remitidos y disposiciones fiscales.
 */
@RestController
@RequestMapping("/api")
public class ExpedienteController {

    private final ExpedienteService expedienteService;
    private final UsuarioService usuarioService;

    public ExpedienteController(ExpedienteService expedienteService, UsuarioService usuarioService) {
        this.expedienteService = expedienteService;
        this.usuarioService = usuarioService;
    }

    /** GET /api/expedientes/{codigo} — expediente completo de una denuncia (ventanas de detalle). */
    @GetMapping("/expedientes/{codigo}")
    public ExpedienteDTO expediente(@PathVariable("codigo") String codigo, HttpSession sesion) {
        Sesion.Datos usuario = Sesion.exigirRol(sesion, Rol.POLICIA, Rol.FISCAL);
        usuarioService.exigirPermiso(usuario, "denuncias");
        return expedienteService.expediente(ExpedienteService.normalizar(codigo));
    }

    /** GET /api/fiscalia/seguimiento — etapa de la Fiscalía en cada denuncia (policía). */
    @GetMapping("/fiscalia/seguimiento")
    public List<SeguimientoFiscalDTO> seguimiento(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.POLICIA);
        return expedienteService.seguimiento();
    }

    /** GET /api/remisiones — informes remitidos a la Fiscalía y su respuesta (policía). */
    @GetMapping("/remisiones")
    public List<RemisionDTO> remisiones(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.POLICIA);
        return expedienteService.remisiones();
    }

    /** GET /api/disposiciones — disposiciones fiscales de las carpetas (fiscal). */
    @GetMapping("/disposiciones")
    public List<DisposicionDTO> disposiciones(HttpSession sesion) {
        Sesion.exigirRol(sesion, Rol.FISCAL);
        return expedienteService.disposiciones();
    }
}
