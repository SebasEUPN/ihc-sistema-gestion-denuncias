package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.IndicadorDTO;
import com.proyecto.denuncias.model.ActividadProgramada;
import com.proyecto.denuncias.model.Actuacion;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.ModuloService;
import com.proyecto.denuncias.service.PanelService;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consultas del panel principal y de los módulos de apoyo de policías y fiscales.
 * (Documentos, remisiones y disposiciones están en DocumentoController y ExpedienteController.)
 */
@RestController
@RequestMapping("/api")
public class ModuloController {

    private final ModuloService moduloService;
    private final PanelService panelService;
    private final UsuarioService usuarioService;

    public ModuloController(ModuloService moduloService, PanelService panelService, UsuarioService usuarioService) {
        this.moduloService = moduloService;
        this.panelService = panelService;
        this.usuarioService = usuarioService;
    }

    /** GET /api/panel/indicadores — las cuatro tarjetas del panel según el perfil. */
    @GetMapping("/panel/indicadores")
    public List<IndicadorDTO> indicadores(HttpSession sesion) {
        return panelService.indicadores(Sesion.actual(sesion).rol());
    }

    /** GET /api/actuaciones — actuaciones policiales o fiscales según quien consulta. */
    @GetMapping("/actuaciones")
    public List<Actuacion> actuaciones(HttpSession sesion) {
        Sesion.Datos usuario = Sesion.exigirRol(sesion, Rol.POLICIA, Rol.FISCAL);
        usuarioService.exigirPermiso(usuario, "tracking");
        return moduloService.actuaciones(usuario.rol());
    }

    /** GET /api/agenda — calendario de actividades (policía) o diligencias (fiscal). */
    @GetMapping("/agenda")
    public List<ActividadProgramada> agenda(HttpSession sesion) {
        Sesion.Datos usuario = Sesion.exigirRol(sesion, Rol.POLICIA, Rol.FISCAL);
        usuarioService.exigirPermiso(usuario, "schedule");
        return moduloService.agenda(usuario.rol());
    }
}
