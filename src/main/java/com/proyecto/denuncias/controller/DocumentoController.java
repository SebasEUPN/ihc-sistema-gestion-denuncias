package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.DocumentoDTO;
import com.proyecto.denuncias.model.Documento;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.ArchivoService;
import com.proyecto.denuncias.service.DocumentoService;
import com.proyecto.denuncias.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Documentos y evidencias (policía y fiscal): listado, subida y archivo.
 */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final ArchivoService archivoService;
    private final UsuarioService usuarioService;

    public DocumentoController(DocumentoService documentoService, ArchivoService archivoService, UsuarioService usuarioService) {
        this.documentoService = documentoService;
        this.archivoService = archivoService;
        this.usuarioService = usuarioService;
    }

    /** Usuario de la sesión, que debe ser policía o fiscal con el módulo de documentos habilitado. */
    private Sesion.Datos usuarioConDocumentos(HttpSession sesion) {
        Sesion.Datos usuario = Sesion.exigirRol(sesion, Rol.POLICIA, Rol.FISCAL);
        usuarioService.exigirPermiso(usuario, "documents");
        return usuario;
    }

    /** GET /api/documentos — todos los documentos y evidencias. */
    @GetMapping
    public List<DocumentoDTO> listar(HttpSession sesion) {
        usuarioConDocumentos(sesion);
        return documentoService.listar();
    }

    /** POST /api/documentos — sube un documento (formulario multipart: denuncia, nombre, descripcion, archivo). */
    @PostMapping
    public DocumentoDTO subir(@RequestParam("denuncia") String denuncia, @RequestParam("nombre") String nombre,
                              @RequestParam(value = "descripcion", required = false) String descripcion,
                              @RequestParam("archivo") MultipartFile archivo, HttpSession sesion) {
        return documentoService.subir(denuncia, nombre, descripcion, archivo, usuarioConDocumentos(sesion));
    }

    /**
     * GET /api/documentos/{codigo}/archivo?descargar=true|false
     * Con descargar=false el archivo se muestra dentro de la página (vista previa);
     * con descargar=true el navegador lo guarda.
     */
    @GetMapping("/{codigo}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable("codigo") String codigo,
                                            @RequestParam(value = "descargar", defaultValue = "false") boolean descargar,
                                            HttpSession sesion) {
        usuarioConDocumentos(sesion);
        Documento documento = documentoService.conArchivo(codigo);
        String tipo = documento.getArchivoTipo();

        // Solo PDF, imágenes (excepto SVG), video y audio se pueden mostrar dentro de la página.
        // Cualquier otro archivo se entrega siempre como descarga.
        boolean visible = tipo.equals("application/pdf") || (tipo.startsWith("image/") && !tipo.contains("svg"))
                || tipo.startsWith("video/") || tipo.startsWith("audio/");
        boolean enLinea = visible && !descargar;

        ContentDisposition disposicion = (enLinea ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(documento.getArchivoNombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(visible ? MediaType.parseMediaType(tipo) : MediaType.APPLICATION_OCTET_STREAM)
                .body(archivoService.cargar(documento.getArchivo()));
    }
}
