package com.proyecto.denuncias.controller;

import com.proyecto.denuncias.dto.ConversacionDTO;
import com.proyecto.denuncias.dto.MensajeRequest;
import com.proyecto.denuncias.model.Mensaje;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.service.ArchivoService;
import com.proyecto.denuncias.service.ChatService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Chat institucional. Cada operación devuelve la lista actualizada de
 * conversaciones para que el frontend vuelva a dibujar la pantalla.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ArchivoService archivoService;
    private final UsuarioService usuarioService;

    public ChatController(ChatService chatService, ArchivoService archivoService, UsuarioService usuarioService) {
        this.chatService = chatService;
        this.archivoService = archivoService;
        this.usuarioService = usuarioService;
    }

    /** Obtiene el usuario de la sesión y comprueba que tenga habilitado el módulo de chat. */
    private Sesion.Datos usuarioConChat(HttpSession sesion) {
        Sesion.Datos usuario = Sesion.actual(sesion);
        usuarioService.exigirPermiso(usuario, "chat");
        return usuario;
    }

    /** GET /api/chat/conversaciones — conversaciones del usuario con sus mensajes. */
    @GetMapping("/conversaciones")
    public List<ConversacionDTO> conversaciones(HttpSession sesion) {
        return chatService.listar(usuarioConChat(sesion).id());
    }

    /** POST /api/chat/conversaciones/{id}/mensajes — envía un mensaje de texto. */
    @PostMapping("/conversaciones/{id}/mensajes")
    public List<ConversacionDTO> enviar(@PathVariable("id") Long id, @RequestBody MensajeRequest datos, HttpSession sesion) {
        Sesion.Datos usuario = usuarioConChat(sesion);
        chatService.enviarTexto(id, usuario.id(), datos.texto());
        return chatService.listar(usuario.id());
    }

    /** POST /api/chat/conversaciones/{id}/adjuntos — envía un archivo (campo "archivo"). */
    @PostMapping("/conversaciones/{id}/adjuntos")
    public List<ConversacionDTO> adjuntar(@PathVariable("id") Long id, @RequestParam("archivo") MultipartFile archivo, HttpSession sesion) {
        Sesion.Datos usuario = usuarioConChat(sesion);
        chatService.enviarAdjunto(id, usuario.id(), archivo);
        return chatService.listar(usuario.id());
    }

    /** POST /api/chat/conversaciones/{id}/leida — marca como leídos los mensajes recibidos. */
    @PostMapping("/conversaciones/{id}/leida")
    public List<ConversacionDTO> marcarLeida(@PathVariable("id") Long id, HttpSession sesion) {
        Sesion.Datos usuario = usuarioConChat(sesion);
        chatService.marcarLeida(id, usuario.id());
        return chatService.listar(usuario.id());
    }

    /**
     * GET /api/chat/adjuntos/{idMensaje} — descarga o muestra un archivo adjunto.
     * Solo pueden verlo los participantes de la conversación.
     */
    @GetMapping("/adjuntos/{idMensaje}")
    public ResponseEntity<Resource> adjunto(@PathVariable("idMensaje") Long idMensaje, HttpSession sesion) {
        Mensaje mensaje = chatService.obtenerAdjunto(idMensaje, usuarioConChat(sesion).id());
        String tipo = mensaje.getAdjuntoTipo();

        // Solo imágenes (excepto SVG), videos y audios se muestran dentro de la página.
        // Cualquier otro archivo se entrega como descarga, para que el navegador nunca
        // ejecute como página un archivo subido por un usuario.
        boolean multimedia = (tipo.startsWith("image/") && !tipo.contains("svg"))
                || tipo.startsWith("video/") || tipo.startsWith("audio/");

        ContentDisposition disposicion = (multimedia ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(mensaje.getAdjuntoNombre(), StandardCharsets.UTF_8)
                .build();

        MediaType tipoRespuesta;
        try {
            tipoRespuesta = multimedia ? MediaType.parseMediaType(tipo) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (RuntimeException e) {
            tipoRespuesta = MediaType.APPLICATION_OCTET_STREAM; // tipo desconocido o mal formado
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(tipoRespuesta)
                .body(archivoService.cargar(mensaje.getAdjuntoArchivo()));
    }
}
