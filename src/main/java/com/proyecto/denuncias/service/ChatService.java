package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.ConversacionDTO;
import com.proyecto.denuncias.dto.MensajeDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Conversacion;
import com.proyecto.denuncias.model.Mensaje;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.repository.ConversacionRepository;
import com.proyecto.denuncias.repository.MensajeRepository;
import com.proyecto.denuncias.repository.UsuarioRepository;
import com.proyecto.denuncias.util.Validar;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lógica del chat: conversaciones del funcionario, envío de mensajes,
 * archivos adjuntos y control de mensajes leídos.
 */
@Service
public class ChatService {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArchivoService archivoService;

    public ChatService(ConversacionRepository conversacionRepository, MensajeRepository mensajeRepository,
                       UsuarioRepository usuarioRepository, ArchivoService archivoService) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.archivoService = archivoService;
    }

    /** Conversaciones en las que participa el usuario, con todos sus mensajes. */
    @Transactional(readOnly = true)
    public List<ConversacionDTO> listar(Long idUsuario) {
        return conversacionRepository.findByUsuarioAIdOrUsuarioBIdOrderByIdAsc(idUsuario, idUsuario).stream()
                .map(c -> aDTO(c, idUsuario))
                .toList();
    }

    /** Envía un mensaje de texto. */
    @Transactional
    public void enviarTexto(Long idConversacion, Long idUsuario, String texto) {
        Conversacion conversacion = obtenerParticipando(idConversacion, idUsuario);
        String contenido = Validar.texto(texto, "Mensaje", 2000);
        mensajeRepository.save(new Mensaje(conversacion, usuario(idUsuario), contenido, LocalDateTime.now(), false));
    }

    /** Envía un archivo adjunto (foto, video, audio o documento). */
    @Transactional
    public void enviarAdjunto(Long idConversacion, Long idUsuario, MultipartFile archivo) {
        Conversacion conversacion = obtenerParticipando(idConversacion, idUsuario);
        String guardado = archivoService.guardar(archivo); // el contenido va al disco

        // Se limpia el nombre original: sin rutas y con un máximo de 200 caracteres
        String nombre = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        nombre = nombre.replaceAll("[\\\\/\\r\\n\"]", "_");
        if (nombre.length() > 200) {
            nombre = nombre.substring(nombre.length() - 200);
        }

        Mensaje mensaje = new Mensaje(conversacion, usuario(idUsuario), nombre, LocalDateTime.now(), false);
        mensaje.setAdjuntoNombre(nombre);
        mensaje.setAdjuntoTipo(archivo.getContentType() == null ? "application/octet-stream" : archivo.getContentType());
        mensaje.setAdjuntoTamano(archivo.getSize());
        mensaje.setAdjuntoArchivo(guardado);
        mensajeRepository.save(mensaje);
    }

    /** Marca como leídos los mensajes que el usuario recibió en una conversación. */
    @Transactional
    public void marcarLeida(Long idConversacion, Long idUsuario) {
        obtenerParticipando(idConversacion, idUsuario);
        for (Mensaje mensaje : mensajeRepository.findByConversacionIdOrderByFechaHoraAscIdAsc(idConversacion)) {
            if (!mensaje.esDe(idUsuario)) {
                mensaje.setLeido(true); // Hibernate guarda el cambio al terminar la transacción
            }
        }
    }

    /** Devuelve el mensaje con adjunto solo si el usuario participa en su conversación. */
    @Transactional(readOnly = true)
    public Mensaje obtenerAdjunto(Long idMensaje, Long idUsuario) {
        Mensaje mensaje = mensajeRepository.findById(idMensaje).orElse(null);
        if (mensaje == null || mensaje.getAdjuntoArchivo() == null
                || !mensaje.getConversacion().participa(idUsuario)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El archivo adjunto no existe.");
        }
        return mensaje;
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------

    private Usuario usuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida."));
    }

    /** Busca la conversación y comprueba que el usuario sea uno de sus participantes. */
    private Conversacion obtenerParticipando(Long idConversacion, Long idUsuario) {
        Conversacion conversacion = conversacionRepository.findById(idConversacion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La conversación no existe."));
        if (!conversacion.participa(idUsuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No participas en esta conversación.");
        }
        return conversacion;
    }

    /** Convierte la conversación al formato que necesita el frontend, vista desde un usuario. */
    private ConversacionDTO aDTO(Conversacion c, Long idUsuario) {
        List<Mensaje> mensajes = mensajeRepository.findByConversacionIdOrderByFechaHoraAscIdAsc(c.getId());

        // Se determina quién es "la otra persona" de la conversación
        String nombre;
        String telefono = "";
        boolean enLinea = false;
        if (c.getCiudadano() != null) {
            nombre = c.getCiudadano().getNombreCorto();
            telefono = c.getCiudadano().getTelefono() == null ? "" : c.getCiudadano().getTelefono();
        } else {
            Usuario otro = c.getUsuarioA().getId().equals(idUsuario) ? c.getUsuarioB() : c.getUsuarioA();
            if (otro == null) {
                // Conversación mal registrada (sin segundo participante): se muestra igualmente
                nombre = "Conversación " + c.getReferencia();
            } else {
                nombre = otro.getNombreCompleto() + " · " + otro.getInstitucion();
                // Se considera "en línea" si inició sesión en los últimos 15 minutos
                enLinea = otro.getUltimoAcceso() != null
                        && otro.getUltimoAcceso().isAfter(LocalDateTime.now().minusMinutes(15));
            }
        }

        // Mensajes recibidos que todavía no fueron leídos
        long noLeidos = mensajes.stream().filter(m -> !m.esDe(idUsuario) && !m.isLeido()).count();

        List<MensajeDTO> lista = mensajes.stream().map(m -> new MensajeDTO(
                m.getId(),
                m.esDe(idUsuario),
                m.getTexto(),
                etiquetaHora(m.getFechaHora()),
                m.getAdjuntoArchivo() == null ? null : new MensajeDTO.Adjunto(
                        m.getAdjuntoNombre(), m.getAdjuntoTipo(),
                        m.getAdjuntoTamano() == null ? 0 : m.getAdjuntoTamano(),
                        "/api/chat/adjuntos/" + m.getId())
        )).toList();

        return new ConversacionDTO(c.getId(), nombre, telefono, c.getReferencia(), enLinea, noLeidos, lista);
    }

    /** "09:12" si el mensaje es de hoy, "Ayer" si es de ayer o "05/11" si es anterior. */
    private String etiquetaHora(LocalDateTime fecha) {
        LocalDate hoy = LocalDate.now();
        if (fecha.toLocalDate().equals(hoy)) {
            return fecha.format(FORMATO_HORA);
        }
        if (fecha.toLocalDate().equals(hoy.minusDays(1))) {
            return "Ayer";
        }
        return fecha.format(FORMATO_DIA);
    }
}
