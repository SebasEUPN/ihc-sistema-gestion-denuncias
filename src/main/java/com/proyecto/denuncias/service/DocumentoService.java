package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.DocumentoDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Denuncia;
import com.proyecto.denuncias.model.Documento;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.repository.DocumentoRepository;
import com.proyecto.denuncias.repository.UsuarioRepository;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.util.PdfSimple;
import com.proyecto.denuncias.util.Validar;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Documentos y evidencias de las denuncias: listado, subida de archivos,
 * entrega del archivo para verlo o descargarlo y archivos de demostración.
 */
@Service
public class DocumentoService {

    /**
     * Tipos de archivo que se pueden subir (extensión -> tipo MIME).
     * El tipo se decide por la extensión y no por lo que diga el navegador.
     */
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"), Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"), Map.entry("gif", "image/gif"),
            Map.entry("mp4", "video/mp4"), Map.entry("webm", "video/webm"),
            Map.entry("mp3", "audio/mpeg"), Map.entry("wav", "audio/wav"), Map.entry("ogg", "audio/ogg"), Map.entry("m4a", "audio/mp4"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("txt", "text/plain"));

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DocumentoRepository documentoRepository;
    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArchivoService archivoService;
    private final AuditoriaService auditoriaService;

    public DocumentoService(DocumentoRepository documentoRepository, DenunciaRepository denunciaRepository,
                            UsuarioRepository usuarioRepository, ArchivoService archivoService,
                            AuditoriaService auditoriaService) {
        this.documentoRepository = documentoRepository;
        this.denunciaRepository = denunciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.archivoService = archivoService;
        this.auditoriaService = auditoriaService;
    }

    /** Todos los documentos, con los datos de su denuncia y de quien los registró. */
    @Transactional(readOnly = true)
    public List<DocumentoDTO> listar() {
        return aDTO(documentoRepository.findAllByOrderByCodigoAsc());
    }

    /** Documentos de una denuncia (los usa el expediente). */
    @Transactional(readOnly = true)
    public List<DocumentoDTO> deDenuncia(String codigoDenuncia) {
        return aDTO(documentoRepository.findByCodigoDenunciaOrderByFechaAscIdAsc(codigoDenuncia));
    }

    /** Documento con archivo, para enviarlo al navegador. */
    @Transactional(readOnly = true)
    public Documento conArchivo(String codigo) {
        Documento documento = documentoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El documento no existe."));
        if (documento.getArchivo() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Este documento no tiene un archivo adjunto.");
        }
        return documento;
    }

    /** Registra un documento nuevo con su archivo (policía o fiscal). */
    @Transactional
    public DocumentoDTO subir(String codigoDenuncia, String nombre, String descripcion, MultipartFile archivo, Sesion.Datos sesion) {
        String denunciaCodigo = Validar.texto(codigoDenuncia, "Denuncia", 20).toUpperCase(Locale.ROOT);
        String titulo = Validar.texto(nombre, "Nombre del documento", 150);
        String detalle = Validar.opcional(descripcion, "Descripción", 500);
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona el archivo que quieres subir.");
        }
        if (!denunciaRepository.findByCodigo(denunciaCodigo).isPresent()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No existe la denuncia " + denunciaCodigo + ".");
        }

        // Nombre original sin rutas y el tipo según la extensión
        String original = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        original = original.replaceAll(".*[\\\\/]", "").replaceAll("[\\r\\n\"]", "_");
        if (original.length() > 200) {
            original = original.substring(original.length() - 200);
        }
        int punto = original.lastIndexOf('.');
        String extension = punto < 0 ? "" : original.substring(punto + 1).toLowerCase(Locale.ROOT);
        String tipo = TIPOS_PERMITIDOS.get(extension);
        if (tipo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Tipo de archivo no permitido. Usa PDF, imágenes, video, audio, Word, Excel o texto.");
        }

        Documento documento = new Documento(siguienteCodigo(), titulo, denunciaCodigo, sesion.codigo(), LocalDate.now(), "Pendiente");
        documento.setDescripcion(detalle);
        documento.asignarArchivo(archivoService.guardar(archivo), original, tipo, archivo.getSize());
        documentoRepository.save(documento);

        auditoriaService.registrar(sesion.codigo(), "Subió el documento " + documento.getCodigo() + " a " + denunciaCodigo, "Documentos");
        return aDTO(List.of(documento)).get(0);
    }

    /**
     * Genera un PDF de demostración para cada documento que todavía no tiene archivo
     * (los documentos de ejemplo del sistema). Lo llama la clase DocumentosDemo al arrancar.
     */
    @Transactional
    public int generarArchivosDemostracion() {
        List<Documento> sinArchivo = documentoRepository.findByArchivoIsNull();
        if (sinArchivo.isEmpty()) {
            return 0;
        }
        Map<String, Denuncia> denuncias = denuncias(sinArchivo.stream().map(Documento::getCodigoDenuncia).collect(Collectors.toSet()));
        Map<String, Usuario> usuarios = usuarios();
        for (Documento d : sinArchivo) {
            byte[] pdf = pdfDemostracion(d, denuncias.get(d.getCodigoDenuncia()), usuarios.get(d.getCodigoResponsable()));
            String nombreArchivo = d.getCodigo() + "_" + d.getNombre().toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9áéíóúñ]+", "_") + ".pdf";
            d.asignarArchivo(archivoService.guardarBytes(pdf), nombreArchivo, "application/pdf", pdf.length);
            if (d.getDescripcion() == null) {
                d.setDescripcion("Documento de demostración generado por el sistema.");
            }
        }
        return sinArchivo.size(); // Hibernate guarda los cambios al terminar la transacción
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------

    /** Palabra clave del nombre del documento -> texto ficticio de su contenido. */
    private static final String[][] CUERPOS = {
            {"acta de intervenci", "En la fecha indicada, el personal policial interviniente se constituyó en el lugar de los hechos a fin de verificar lo denunciado. Se procedió a identificar a las personas presentes, a registrar las condiciones del lugar y a recabar la información preliminar necesaria para la investigación."},
            {"constataci", "Se constató el estado del lugar y de los bienes señalados por el denunciante, dejando registro de las condiciones observadas y de las personas presentes durante la diligencia."},
            {"recojo de evidencias", "Se recogieron, lacraron y rotularon las evidencias halladas en el lugar, garantizando su cadena de custodia para su posterior remisión a la unidad de criminalística."},
            {"comiso", "Se procedió al comiso, pesaje y lacrado de la sustancia encontrada, realizándose la prueba de campo correspondiente en presencia del intervenido."},
            {"declaraci", "La persona declarante, debidamente identificada, manifestó libre y voluntariamente su versión de los hechos materia de la denuncia, respondiendo a las preguntas formuladas por el personal a cargo. Leída su declaración, se ratificó en su contenido."},
            {"fotogr", "Registro fotográfico del lugar de los hechos tomado durante la inspección. Las imágenes muestran el estado del lugar, los accesos y los indicios encontrados (contenido ilustrativo)."},
            {"certificado m", "Del reconocimiento médico legal practicado al agraviado se concluye que presenta lesiones compatibles con lo denunciado, requiriendo los días de atención facultativa e incapacidad médico legal indicados por el perito (contenido ficticio)."},
            {"pericial", "El perito, luego del examen de las muestras e indicios remitidos, emite las conclusiones técnicas que se detallan para su valoración por la autoridad competente."},
            {"valoraci", "Aplicada la ficha de valoración de riesgo a la víctima, se determina el nivel de riesgo y se recomiendan las medidas de protección correspondientes."},
            {"parte policial", "Se da cuenta de las diligencias realizadas por la unidad policial con relación a la denuncia, consignándose a las personas intervenidas y las acciones pendientes."},
            {"informe", "El presente informe resume las diligencias policiales realizadas, los elementos recabados y las conclusiones preliminares, y se remite a la Fiscalía para la evaluación correspondiente."},
            {"oficio", "Por medio del presente se remite a la Fiscalía lo actuado en la denuncia, adjuntándose las actas, declaraciones y demás documentos obtenidos."},
            {"constancia", "Se deja constancia de que el ciudadano presentó su denuncia, la cual fue registrada con el código indicado para su consulta y seguimiento."},
            {"reporte", "Reporte con la información consultada a las entidades correspondientes, incorporado al expediente para el esclarecimiento de los hechos."},
            {"citaci", "Se cita a la persona indicada a comparecer en el despacho fiscal en la fecha y hora señaladas, a fin de rendir su declaración. Su inasistencia injustificada podrá dar lugar a las medidas que establece la ley."},
            {"requerimiento", "El despacho fiscal requiere a la unidad policial ampliar la información y realizar las diligencias complementarias detalladas, en el plazo señalado."},
            {"disposici", "Vistos los actuados, el despacho fiscal dispone continuar con las diligencias preliminares necesarias para el esclarecimiento de los hechos y notificar a las partes conforme a ley."}};

    private String cuerpoDemostracion(String nombre) {
        for (String[] par : CUERPOS) {
            if (nombre.contains(par[0])) {
                return par[1];
            }
        }
        return "Documento incorporado al expediente de la denuncia para su evaluación por la autoridad competente.";
    }

    private List<DocumentoDTO> aDTO(List<Documento> documentos) {
        Map<String, Denuncia> denuncias = denuncias(documentos.stream().map(Documento::getCodigoDenuncia).collect(Collectors.toSet()));
        Map<String, Usuario> usuarios = usuarios();
        return documentos.stream()
                .map(d -> DocumentoDTO.de(d, denuncias.get(d.getCodigoDenuncia()), usuarios.get(d.getCodigoResponsable())))
                .toList();
    }

    private Map<String, Denuncia> denuncias(Collection<String> codigos) {
        return codigos.isEmpty() ? Map.of() : denunciaRepository.findByCodigoIn(codigos).stream()
                .collect(Collectors.toMap(Denuncia::getCodigo, Function.identity()));
    }

    private Map<String, Usuario> usuarios() {
        return usuarioRepository.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(Usuario::getCodigo, Function.identity()));
    }

    /** Siguiente código libre: DOC-005, DOC-006... */
    private String siguienteCodigo() {
        long numero = documentoRepository.count() + 1;
        String codigo = String.format("DOC-%03d", numero);
        while (documentoRepository.existsByCodigo(codigo)) {
            codigo = String.format("DOC-%03d", ++numero);
        }
        return codigo;
    }

    /** PDF ficticio con los datos del documento y de su denuncia. */
    private byte[] pdfDemostracion(Documento d, Denuncia denuncia, Usuario responsable) {
        boolean fiscal = d.getCodigoResponsable().startsWith("FN");
        PdfSimple pdf = new PdfSimple(false);
        pdf.subtitulo(fiscal ? "MINISTERIO PÚBLICO - FISCALÍA DE LA NACIÓN" : "POLICÍA NACIONAL DEL PERÚ");
        pdf.titulo(d.getNombre().toUpperCase(Locale.ROOT));
        pdf.parrafo("DOCUMENTO DE DEMOSTRACIÓN · PROTOTIPO ACADÉMICO · Todos los datos son ficticios.", 8.5f);
        pdf.separador();
        pdf.dato("Código del documento", d.getCodigo());
        pdf.dato("Denuncia", d.getCodigoDenuncia());
        if (denuncia != null) {
            pdf.dato("Tipo de denuncia", denuncia.getTipoDelito());
            pdf.dato("Denunciante", denuncia.getCiudadano().getNombreCompleto() + " (DNI " + denuncia.getCiudadano().getDni() + ")");
            pdf.dato("Lugar de los hechos", denuncia.getLugarHechos());
            pdf.dato("Fecha de registro de la denuncia", denuncia.getFechaRegistro().format(FECHA));
            if (denuncia.getCarpetaFiscal() != null) {
                pdf.dato("Carpeta fiscal", denuncia.getCarpetaFiscal());
            }
        }
        pdf.dato("Fecha del documento", d.getFecha().format(FECHA));
        pdf.dato("Responsable", d.getCodigoResponsable() + (responsable == null ? "" : " - " + responsable.getNombreCompleto()));
        pdf.dato("Estado", d.getEstado());
        pdf.separador();

        // Texto del cuerpo según el tipo de documento (se busca una palabra clave en el nombre)
        String cuerpo = cuerpoDemostracion(d.getNombre().toLowerCase(Locale.ROOT));
        pdf.subtitulo("Contenido");
        pdf.parrafo(cuerpo, 10);
        if (denuncia != null) {
            pdf.subtitulo("Hechos denunciados");
            pdf.parrafo(denuncia.getDescripcion(), 10);
        }
        pdf.espacio(40);
        pdf.parrafo("______________________________", 10);
        pdf.parrafo(responsable == null ? d.getCodigoResponsable() : responsable.getNombreCompleto() + " · " + d.getCodigoResponsable(), 9);
        return pdf.generar("Sistema de Gestión y Seguimiento de Denuncias · Documento ficticio de demostración");
    }
}
