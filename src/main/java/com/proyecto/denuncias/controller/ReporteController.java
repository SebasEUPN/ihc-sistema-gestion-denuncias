package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.OpcionesReporteDTO;
import com.proyecto.denuncias.dto.ReporteFilaDTO;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.AuditoriaService;
import com.proyecto.denuncias.service.ReporteService;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reportes de denuncias con filtros, para los tres perfiles.
 * Los mismos filtros sirven para ver el resultado, exportarlo a PDF o a Excel.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public ReporteController(ReporteService reporteService, UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.reporteService = reporteService;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    /** Usuario de la sesión con el módulo de reportes habilitado (el administrador siempre lo tiene). */
    private Sesion.Datos usuarioConReportes(HttpSession sesion) {
        Sesion.Datos usuario = Sesion.actual(sesion);
        usuarioService.exigirPermiso(usuario, "reports");
        return usuario;
    }

    /** GET /api/reportes/opciones — valores para los filtros. */
    @GetMapping("/opciones")
    public OpcionesReporteDTO opciones(HttpSession sesion) {
        usuarioConReportes(sesion);
        return reporteService.opciones();
    }

    /** GET /api/reportes/denuncias?delito=&estadoPolicial=&estadoFiscal=&provincia=&distrito=&desde=&hasta=&texto= */
    @GetMapping("/denuncias")
    public List<ReporteFilaDTO> denuncias(
            @RequestParam(value = "delito", required = false) String delito,
            @RequestParam(value = "estadoPolicial", required = false) String estadoPolicial,
            @RequestParam(value = "estadoFiscal", required = false) String estadoFiscal,
            @RequestParam(value = "provincia", required = false) String provincia,
            @RequestParam(value = "distrito", required = false) String distrito,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "texto", required = false) String texto,
            HttpSession sesion) {
        usuarioConReportes(sesion);
        return reporteService.filtrar(new ReporteService.Filtro(delito, estadoPolicial, estadoFiscal, provincia, distrito, desde, hasta, texto));
    }

    /** GET /api/reportes/denuncias/pdf?... — el mismo reporte como archivo PDF. */
    @GetMapping("/denuncias/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(value = "delito", required = false) String delito,
            @RequestParam(value = "estadoPolicial", required = false) String estadoPolicial,
            @RequestParam(value = "estadoFiscal", required = false) String estadoFiscal,
            @RequestParam(value = "provincia", required = false) String provincia,
            @RequestParam(value = "distrito", required = false) String distrito,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "texto", required = false) String texto,
            HttpSession sesion) {
        Sesion.Datos usuario = usuarioConReportes(sesion);
        ReporteService.Filtro filtro = new ReporteService.Filtro(delito, estadoPolicial, estadoFiscal, provincia, distrito, desde, hasta, texto);
        List<ReporteFilaDTO> filas = reporteService.filtrar(filtro);
        byte[] archivo = reporteService.pdf(filas, filtro, funcionario(usuario));
        auditoriaService.registrar(usuario.codigo(), "Exportó a PDF un reporte de " + filas.size() + " denuncias", "Reportes");
        return descarga(archivo, "reporte_denuncias_" + LocalDate.now() + ".pdf", MediaType.APPLICATION_PDF);
    }

    /** GET /api/reportes/denuncias/excel?... — el mismo reporte como archivo Excel (.xlsx). */
    @GetMapping("/denuncias/excel")
    public ResponseEntity<byte[]> excel(
            @RequestParam(value = "delito", required = false) String delito,
            @RequestParam(value = "estadoPolicial", required = false) String estadoPolicial,
            @RequestParam(value = "estadoFiscal", required = false) String estadoFiscal,
            @RequestParam(value = "provincia", required = false) String provincia,
            @RequestParam(value = "distrito", required = false) String distrito,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "texto", required = false) String texto,
            HttpSession sesion) {
        Sesion.Datos usuario = usuarioConReportes(sesion);
        ReporteService.Filtro filtro = new ReporteService.Filtro(delito, estadoPolicial, estadoFiscal, provincia, distrito, desde, hasta, texto);
        List<ReporteFilaDTO> filas = reporteService.filtrar(filtro);
        byte[] archivo = reporteService.excel(filas, filtro, funcionario(usuario));
        auditoriaService.registrar(usuario.codigo(), "Exportó a Excel un reporte de " + filas.size() + " denuncias", "Reportes");
        return descarga(archivo, "reporte_denuncias_" + LocalDate.now() + ".xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    // ------------------------------------------------------------------

    /** "Carlos Mendoza Ruiz (PNP-2045)" */
    private String funcionario(Sesion.Datos usuario) {
        return usuarioService.obtener(usuario.id()).getNombreCompleto() + " (" + usuario.codigo() + ")";
    }

    /** Respuesta que el navegador guarda como archivo. */
    private ResponseEntity<byte[]> descarga(byte[] contenido, String nombre, MediaType tipo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(tipo)
                .body(contenido);
    }
}
