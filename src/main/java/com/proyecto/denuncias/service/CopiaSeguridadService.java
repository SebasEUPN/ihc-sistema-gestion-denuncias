package com.proyecto.denuncias.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.denuncias.dto.ConfiguracionCopiaDTO;
import com.proyecto.denuncias.dto.CopiaDTO;
import com.proyecto.denuncias.dto.CrearCopiaRequest;
import com.proyecto.denuncias.dto.RestauracionDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.ConfiguracionCopia;
import com.proyecto.denuncias.model.CopiaSeguridad;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.repository.ConfiguracionCopiaRepository;
import com.proyecto.denuncias.repository.CopiaSeguridadRepository;
import com.proyecto.denuncias.repository.UsuarioRepository;
import com.proyecto.denuncias.util.Validar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Copias de seguridad del administrador: crear, programar, listar, descargar y restaurar.
 *
 * Una copia es un archivo JSON comprimido (.json.gz) con las filas de las tablas
 * elegidas. Se guarda en la tabla "archivo_almacenado" y su registro en
 * "copia_seguridad", enlazado con el administrador que la generó.
 *
 * Restaurar devuelve cada fila de la copia al valor que tenía cuando se generó
 * (si la fila fue borrada, se vuelve a crear). Los registros creados DESPUÉS de
 * la copia se conservan: la restauración nunca borra información.
 */
@Service
public class CopiaSeguridadService {

    private static final Logger log = LoggerFactory.getLogger(CopiaSeguridadService.class);

    public static final String MANUAL = "Manual";
    public static final String AUTOMATICA = "Automática";

    /** Tablas que se pueden respaldar, con su clave primaria. El orden importa: primero las tablas "padre". */
    private static final Map<String, List<String>> CLAVES = new LinkedHashMap<>();
    static {
        CLAVES.put("usuario", List.of("id_usuario"));
        CLAVES.put("policia", List.of("id_usuario"));
        CLAVES.put("fiscal", List.of("id_usuario"));
        CLAVES.put("admin", List.of("id_usuario"));
        CLAVES.put("usuario_permiso", List.of("id_usuario", "modulo"));
        CLAVES.put("ciudadano", List.of("id_ciudadano"));
        CLAVES.put("denuncia", List.of("id_denuncia"));
        CLAVES.put("seguimiento_denuncia", List.of("id_seguimiento"));
        CLAVES.put("actuacion", List.of("id_actuacion"));
        CLAVES.put("documento", List.of("id_documento"));
        CLAVES.put("remision_fiscal", List.of("id_remision"));
        CLAVES.put("disposicion_fiscal", List.of("id_disposicion"));
        CLAVES.put("actividad_programada", List.of("id_actividad"));
        CLAVES.put("registro_actividad", List.of("id_registro"));
        CLAVES.put("conversacion", List.of("id_conversacion"));
        CLAVES.put("mensaje", List.of("id_mensaje"));
    }

    /** Tablas que incluye cada tipo de copia. */
    private static final Map<String, List<String>> TABLAS_POR_TIPO = Map.of(
            "COMPLETA", new ArrayList<>(CLAVES.keySet()),
            "DENUNCIAS", List.of("ciudadano", "denuncia", "seguimiento_denuncia", "actuacion", "documento",
                    "remision_fiscal", "disposicion_fiscal"),
            "USUARIOS", List.of("usuario", "policia", "fiscal", "admin", "usuario_permiso"));

    private static final Map<String, String> NOMBRE_TIPO = Map.of(
            "COMPLETA", "Completa",
            "DENUNCIAS", "Denuncias y seguimiento",
            "USUARIOS", "Usuarios y permisos");

    private static final Map<String, Integer> DIAS_POR_FRECUENCIA = Map.of("DIARIA", 1, "SEMANAL", 7, "MENSUAL", 30);

    private final CopiaSeguridadRepository copiaRepository;
    private final ConfiguracionCopiaRepository configuracionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArchivoService archivoService;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate soloLectura;

    public CopiaSeguridadService(CopiaSeguridadRepository copiaRepository,
                                 ConfiguracionCopiaRepository configuracionRepository,
                                 UsuarioRepository usuarioRepository, ArchivoService archivoService,
                                 AuditoriaService auditoriaService, PasswordEncoder passwordEncoder,
                                 JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager transacciones) {
        this.copiaRepository = copiaRepository;
        this.configuracionRepository = configuracionRepository;
        this.usuarioRepository = usuarioRepository;
        this.archivoService = archivoService;
        this.auditoriaService = auditoriaService;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.json = json;
        this.transaccion = new TransactionTemplate(transacciones);
        this.soloLectura = new TransactionTemplate(transacciones);
        this.soloLectura.setReadOnly(true);
    }

    // ------------------------------------------------------------------
    // 1. Crear copia de seguridad
    // ------------------------------------------------------------------

    /** Crea una copia a pedido del administrador (formulario "Crear copia de seguridad"). */
    public CopiaDTO crear(Long idAdmin, CrearCopiaRequest datos) {
        String nombre = Validar.formato(datos.nombre(), "Nombre de la copia", "[A-Za-z0-9ÁÉÍÓÚáéíóúÑñ _.-]{3,80}",
                "debe tener de 3 a 80 caracteres (letras, números, espacios, guion, punto o guion bajo)");
        String tipo = datos.tipo() == null ? "" : datos.tipo().trim().toUpperCase();
        if (!TABLAS_POR_TIPO.containsKey(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona un tipo de copia de seguridad válido.");
        }
        String descripcion = Validar.opcional(datos.descripcion(), "Descripción", 300);
        if (copiaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una copia con ese nombre. Usa un nombre distinto.");
        }
        Usuario admin = administrador(idAdmin);
        CopiaSeguridad copia = generar(admin, nombre, tipo, descripcion, MANUAL);
        auditoriaService.registrar(admin.getCodigo(), CopiaSeguridad.COMPLETADA.equals(copia.getEstado())
                ? "Creó la copia de seguridad " + nombre
                : "Falló la copia de seguridad " + nombre, "Copias de seguridad");
        if (!CopiaSeguridad.COMPLETADA.equals(copia.getEstado())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar la copia. Quedó registrada como fallida en el historial.");
        }
        return soloLectura.execute(t -> dto(copiaRepository.findById(copia.getId()).orElseThrow()));
    }

    /** Genera el archivo de la copia y guarda su registro (completada o fallida). */
    private CopiaSeguridad generar(Usuario admin, String nombre, String tipo, String descripcion, String origen) {
        CopiaSeguridad copia = new CopiaSeguridad(admin, nombre, tipo, descripcion, origen);
        try {
            // Todas las tablas se leen dentro de una misma transacción de solo lectura
            Map<String, Object> contenido = soloLectura.execute(t -> leerTablas(tipo, nombre));
            int registros = 0;
            for (Object filas : ((Map<?, ?>) contenido.get("tablas")).values()) {
                registros += ((List<?>) filas).size();
            }
            byte[] comprimido = comprimir(json.writeValueAsBytes(contenido));
            String archivo = archivoService.guardarBytes(comprimido);
            copia.completar(archivo, comprimido.length, registros);
        } catch (Exception e) {
            log.error("No se pudo generar la copia de seguridad {}", nombre, e);
        }
        return copiaRepository.save(copia);
    }

    /** Lee las filas de cada tabla del tipo de copia. Todos los valores se guardan como texto. */
    private Map<String, Object> leerTablas(String tipo, String nombre) {
        Map<String, Object> tablas = new LinkedHashMap<>();
        for (String tabla : TABLAS_POR_TIPO.get(tipo)) {
            // El nombre de la tabla viene de la lista fija de esta clase, no del usuario
            String orden = String.join(", ", CLAVES.get(tabla));
            List<Map<String, String>> filas = new ArrayList<>();
            for (Map<String, Object> fila : jdbc.queryForList("SELECT * FROM " + tabla + " ORDER BY " + orden)) {
                Map<String, String> textos = new LinkedHashMap<>();
                fila.forEach((columna, valor) -> textos.put(columna, texto(valor)));
                filas.add(textos);
            }
            tablas.put(tabla, filas);
        }
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("formato", "sgd-copia-v1");
        contenido.put("sistema", "Sistema de gestión de denuncias (prototipo académico)");
        contenido.put("nombre", nombre);
        contenido.put("tipo", tipo);
        contenido.put("generado", LocalDateTime.now().toString());
        contenido.put("tablas", tablas);
        return contenido;
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof java.sql.Timestamp fechaHora) {
            return fechaHora.toLocalDateTime().toString();
        }
        return valor.toString();
    }

    // ------------------------------------------------------------------
    // 2. Copias automáticas
    // ------------------------------------------------------------------

    /** Programación actual (o los valores por defecto si todavía no se guardó ninguna). */
    public ConfiguracionCopiaDTO configuracion() {
        return soloLectura.execute(t -> {
            LocalDateTime ultima = copiaRepository
                    .findFirstByOrigenAndEstadoOrderByFechaHoraDesc(AUTOMATICA, CopiaSeguridad.COMPLETADA)
                    .map(CopiaSeguridad::getFechaHora).orElse(null);
            return configuracionRepository.findById(ConfiguracionCopia.ID_UNICO)
                    .map(c -> new ConfiguracionCopiaDTO(c.isActivo(), c.getFrecuencia(), c.getHora(), c.getDiasConservacion(),
                            c.getAdmin().getCodigo() + " · " + c.getAdmin().getNombreCompleto(), c.getFechaActualizacion(), ultima))
                    .orElse(new ConfiguracionCopiaDTO(false, "DIARIA", "02:00", 30, null, null, ultima));
        });
    }

    /** Guarda la programación de las copias automáticas. */
    public ConfiguracionCopiaDTO guardarConfiguracion(Long idAdmin, ConfiguracionCopiaDTO datos) {
        String frecuencia = datos.frecuencia() == null ? "" : datos.frecuencia().trim().toUpperCase();
        if (!DIAS_POR_FRECUENCIA.containsKey(frecuencia)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona una frecuencia válida.");
        }
        String hora = Validar.formato(datos.hora(), "Hora programada", "([01]\\d|2[0-3]):[0-5]\\d", "debe tener el formato HH:mm");
        if (datos.diasConservacion() < 1 || datos.diasConservacion() > 365) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tiempo de conservación debe estar entre 1 y 365 días.");
        }
        Usuario admin = administrador(idAdmin);
        transaccion.executeWithoutResult(t -> {
            ConfiguracionCopia c = configuracionRepository.findById(ConfiguracionCopia.ID_UNICO).orElseGet(ConfiguracionCopia::new);
            c.actualizar(admin, datos.activo(), frecuencia, hora, datos.diasConservacion());
            configuracionRepository.save(c);
        });
        auditoriaService.registrar(admin.getCodigo(), datos.activo()
                ? "Programó copias automáticas (" + frecuencia.toLowerCase() + ", " + hora + ")"
                : "Desactivó las copias automáticas", "Copias de seguridad");
        return configuracion();
    }

    /**
     * Cada 5 minutos revisa si toca generar la copia automática. Como el servidor
     * gratuito se duerme cuando nadie lo usa, la copia se genera en cuanto el
     * sistema está despierto después de la hora programada.
     */
    @Scheduled(initialDelayString = "${app.copias.revision-inicial-ms:120000}", fixedDelayString = "${app.copias.revision-ms:300000}")
    public void ejecutarCopiasAutomaticas() {
        try {
            Object[] programada = soloLectura.execute(t -> configuracionRepository.findById(ConfiguracionCopia.ID_UNICO)
                    .filter(ConfiguracionCopia::isActivo)
                    .map(c -> new Object[] {c.getAdmin().getId(), c.getFrecuencia(), c.getHora(), c.getDiasConservacion()})
                    .orElse(null));
            if (programada == null) {
                return;
            }
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime ultima = soloLectura.execute(t -> copiaRepository
                    .findFirstByOrigenAndEstadoOrderByFechaHoraDesc(AUTOMATICA, CopiaSeguridad.COMPLETADA)
                    .map(CopiaSeguridad::getFechaHora).orElse(null));
            int dias = DIAS_POR_FRECUENCIA.get((String) programada[1]);
            boolean pasoLaHora = !ahora.toLocalTime().isBefore(LocalTime.parse((String) programada[2]));
            boolean tocaHoy = ultima == null || !ultima.toLocalDate().plusDays(dias).isAfter(LocalDate.now());
            if (pasoLaHora && tocaHoy) {
                Usuario admin = administrador((Long) programada[0]);
                String nombre = "Auto_" + ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
                CopiaSeguridad copia = generar(admin, nombre, "COMPLETA", "Copia automática programada", AUTOMATICA);
                auditoriaService.registrar(admin.getCodigo(), "Copia automática " + nombre + ": " + copia.getEstado().toLowerCase(),
                        "Copias de seguridad");
            }
            // Las copias automáticas más antiguas que el tiempo de conservación se eliminan
            LocalDateTime limite = ahora.minusDays((Integer) programada[3]);
            for (CopiaSeguridad vieja : copiaRepository.findByOrigenAndFechaHoraBefore(AUTOMATICA, limite)) {
                archivoService.eliminar(vieja.getArchivo());
                copiaRepository.delete(vieja);
            }
        } catch (Exception e) {
            log.error("Error al revisar las copias automáticas", e);
        }
    }

    // ------------------------------------------------------------------
    // 3. Historial y descarga
    // ------------------------------------------------------------------

    /** Historial de copias, de la más reciente a la más antigua. */
    public List<CopiaDTO> listar() {
        return soloLectura.execute(t -> copiaRepository.findAllByOrderByFechaHoraDescIdDesc().stream().map(this::dto).toList());
    }

    /** Archivo de una copia para descargarlo: [nombre de archivo, contenido]. */
    public Object[] archivo(Long id) {
        CopiaSeguridad copia = disponible(id);
        Resource contenido = archivoService.cargar(copia.getArchivo());
        return new Object[] {copia.getNombre().replaceAll("[^A-Za-z0-9_.-]", "_") + ".json.gz", contenido};
    }

    // ------------------------------------------------------------------
    // 4. Restaurar
    // ------------------------------------------------------------------

    /** Restaura una copia. El administrador confirma escribiendo su contraseña. */
    public RestauracionDTO restaurar(Long id, Long idAdmin, String password) {
        Usuario admin = administrador(idAdmin);
        if (password == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La contraseña no es correcta. No se restauró nada.");
        }
        CopiaSeguridad copia = disponible(id);
        Map<String, List<Map<String, String>>> tablas;
        try {
            byte[] datos = descomprimir(archivoService.cargar(copia.getArchivo()).getContentAsByteArray());
            Map<String, Object> contenido = json.readValue(datos, new TypeReference<Map<String, Object>>() { });
            tablas = json.convertValue(contenido.get("tablas"), new TypeReference<Map<String, List<Map<String, String>>>>() { });
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "El archivo de la copia está dañado y no se puede restaurar.");
        }
        int[] total = {0, 0};
        try {
            // Todo dentro de una sola transacción: si algo falla, no se cambia nada
            transaccion.executeWithoutResult(t -> {
                for (String tabla : CLAVES.keySet()) {            // siempre en el orden padre -> hijo
                    List<Map<String, String>> filas = tablas == null ? null : tablas.get(tabla);
                    if (filas != null && !filas.isEmpty()) {
                        total[0]++;
                        total[1] += restaurarTabla(tabla, filas);
                    }
                }
            });
        } catch (Exception e) {
            log.error("No se pudo restaurar la copia {}", copia.getNombre(), e);
            auditoriaService.registrar(admin.getCodigo(), "Falló la restauración de la copia " + copia.getNombre(), "Copias de seguridad");
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se pudo restaurar la copia porque algunos datos actuales entran en conflicto con ella. No se cambió nada.");
        }
        auditoriaService.registrar(admin.getCodigo(), "Restauró la copia de seguridad " + copia.getNombre(), "Copias de seguridad");
        return new RestauracionDTO(copia.getNombre(), total[0], total[1],
                "Se restauraron " + total[1] + " registros de " + total[0] + " tablas desde la copia " + copia.getNombre() + ".");
    }

    /**
     * Inserta cada fila de la copia; si la fila ya existe (misma clave primaria),
     * la actualiza con los valores de la copia.
     */
    private int restaurarTabla(String tabla, List<Map<String, String>> filas) {
        // Tipo de cada columna que la tabla tiene HOY (solo se restauran columnas que siguen existiendo)
        Map<String, String> tipos = new LinkedHashMap<>();
        jdbc.query("SELECT column_name, data_type FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() AND table_name = ? ORDER BY ordinal_position",
                rs -> { tipos.put(rs.getString(1), rs.getString(2)); }, tabla);
        List<String> columnas = tipos.keySet().stream().filter(filas.get(0)::containsKey).toList();
        List<String> clave = CLAVES.get(tabla);
        if (!columnas.containsAll(clave)) {
            throw new IllegalStateException("La copia no tiene la clave de la tabla " + tabla);
        }
        // Los valores viajan como texto y PostgreSQL los convierte al tipo de cada columna
        String valores = String.join(", ", columnas.stream().map(c -> "CAST(? AS " + tipos.get(c) + ")").toList());
        List<String> otras = columnas.stream().filter(c -> !clave.contains(c)).toList();
        String alExistir = otras.isEmpty() ? "DO NOTHING"
                : "DO UPDATE SET " + String.join(", ", otras.stream().map(c -> c + " = EXCLUDED." + c).toList());
        String sql = "INSERT INTO " + tabla + " (" + String.join(", ", columnas) + ") VALUES (" + valores + ") "
                + "ON CONFLICT (" + String.join(", ", clave) + ") " + alExistir;
        List<Object[]> lote = filas.stream().map(f -> columnas.stream().map(f::get).toArray()).toList();
        jdbc.batchUpdate(sql, lote);
        return lote.size();
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------

    private CopiaDTO dto(CopiaSeguridad copia) {
        return CopiaDTO.de(copia, NOMBRE_TIPO.getOrDefault(copia.getTipo(), copia.getTipo()));
    }

    private Usuario administrador(Long id) {
        Usuario admin = usuarioRepository.findById(id).orElse(null);
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un administrador puede gestionar las copias de seguridad.");
        }
        return admin;
    }

    /** Copia completada que todavía tiene su archivo. */
    private CopiaSeguridad disponible(Long id) {
        CopiaSeguridad copia = copiaRepository.findById(id).orElse(null);
        if (copia == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "La copia de seguridad no existe.");
        }
        if (!CopiaSeguridad.COMPLETADA.equals(copia.getEstado()) || copia.getArchivo() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Esa copia no tiene un archivo disponible.");
        }
        return copia;
    }

    private static byte[] comprimir(byte[] datos) throws java.io.IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(salida)) {
            gzip.write(datos);
        }
        return salida.toByteArray();
    }

    private static byte[] descomprimir(byte[] datos) throws java.io.IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(datos))) {
            return gzip.readAllBytes();
        }
    }
}
