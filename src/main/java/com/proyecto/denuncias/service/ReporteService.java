package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.InactividadDTO;
import com.proyecto.denuncias.dto.OpcionesReporteDTO;
import com.proyecto.denuncias.dto.ReporteFilaDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Denuncia;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.util.PdfSimple;
import com.proyecto.denuncias.util.Ubicacion;
import com.proyecto.denuncias.util.XlsxSimple;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Reportes de denuncias con filtros (tipo de delito, estados, provincia,
 * distrito, fechas y texto libre) y su exportación a PDF y Excel.
 */
@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Filtros de la consulta. Un filtro vacío (null o "") no se aplica. */
    public record Filtro(String delito, String estadoPolicial, String estadoFiscal, String provincia, String distrito,
                         LocalDate desde, LocalDate hasta, String texto, String situacion) {

        /** Descripción legible de los filtros usados (se imprime en el PDF y en el Excel). */
        public String descripcion() {
            List<String> partes = new ArrayList<>();
            if (lleno(delito)) partes.add("Delito: " + delito);
            if (lleno(estadoPolicial)) partes.add("Estado PNP: " + estadoPolicial);
            if (lleno(estadoFiscal)) partes.add("Estado Fiscalía: " + estadoFiscal);
            if (lleno(provincia)) partes.add("Provincia: " + provincia);
            if (lleno(distrito)) partes.add("Distrito: " + distrito);
            if (desde != null) partes.add("Desde: " + desde.format(FECHA));
            if (hasta != null) partes.add("Hasta: " + hasta.format(FECHA));
            if (lleno(texto)) partes.add("Texto: \"" + texto.trim() + "\"");
            if (lleno(situacion)) partes.add("Inactividad: " + nombreSituacion(situacion));
            return partes.isEmpty() ? "Todas las denuncias (sin filtros)" : String.join(" · ", partes);
        }
    }

    /** Valor del filtro de inactividad que agrupa Crítica y Prioritaria (15 días o más). */
    public static final String POR_VENCER = "POR_VENCER";

    private final DenunciaRepository denunciaRepository;
    private final InactividadService inactividadService;

    private final JdbcTemplate jdbc;

    public ReporteService(DenunciaRepository denunciaRepository, InactividadService inactividadService, JdbcTemplate jdbc) {
        this.denunciaRepository = denunciaRepository;
        this.inactividadService = inactividadService;
        this.jdbc = jdbc;
    }

    /** "Crítica" o, para el valor POR_VENCER, "Por vencer (15 días o más)". */
    static String nombreSituacion(String situacion) {
        return POR_VENCER.equals(situacion) ? "Por vencer (" + InactividadService.DIAS_POR_VENCER + " días o más)" : situacion;
    }

    /** ¿La inactividad cumple el filtro? */
    private static boolean cumple(String filtro, InactividadService.Inactividad i) {
        if (!lleno(filtro)) return true;
        if (POR_VENCER.equals(filtro)) {
            return !InactividadService.CONCLUIDA.equals(i.situacion()) && i.dias() >= InactividadService.DIAS_POR_VENCER;
        }
        return filtro.equals(i.situacion());
    }

    /** Valores para llenar los filtros: delitos, estados y provincias con sus distritos. */
    public OpcionesReporteDTO opciones() {
        TreeSet<String> delitos = new TreeSet<>(), estadosPnp = new TreeSet<>(), estadosFiscalia = new TreeSet<>();
        Map<String, TreeSet<String>> provincias = new TreeMap<>();
        for (Denuncia d : denunciaRepository.listarConCiudadano()) {
            delitos.add(d.getTipoDelito());
            estadosPnp.add(d.getEstadoPolicial());
            estadosFiscalia.add(d.getEstadoFiscal());
            Ubicacion.Partes lugar = Ubicacion.de(d.getLugarHechos());
            provincias.computeIfAbsent(lugar.provincia(), k -> new TreeSet<>()).add(lugar.distrito());
        }
        return new OpcionesReporteDTO(new ArrayList<>(delitos), new ArrayList<>(estadosPnp), new ArrayList<>(estadosFiscalia),
                provincias.entrySet().stream().map(e -> new OpcionesReporteDTO.Provincia(e.getKey(), new ArrayList<>(e.getValue()))).toList());
    }

    /** Denuncias que cumplen todos los filtros, de la más reciente a la más antigua. */
    public List<ReporteFilaDTO> filtrar(Filtro f) {
        if (f.desde() != null && f.hasta() != null && f.desde().isAfter(f.hasta())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (lleno(f.situacion()) && !POR_VENCER.equals(f.situacion()) && !InactividadService.SITUACIONES.contains(f.situacion())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona una situación de inactividad válida.");
        }
        String texto = lleno(f.texto()) ? f.texto().trim().toLowerCase(Locale.ROOT) : null;
        Map<String, InactividadService.Inactividad> inactividad = inactividadService.todas();
        List<ReporteFilaDTO> filas = new ArrayList<>();
        for (Denuncia d : denunciaRepository.listarConCiudadano()) {
            Ubicacion.Partes lugar = Ubicacion.de(d.getLugarHechos());
            LocalDate fecha = d.getFechaRegistro().toLocalDate();
            String ciudadano = d.getCiudadano().getNombreCompleto();
            if (lleno(f.delito()) && !f.delito().equals(d.getTipoDelito())) continue;
            if (lleno(f.estadoPolicial()) && !f.estadoPolicial().equals(d.getEstadoPolicial())) continue;
            if (lleno(f.estadoFiscal()) && !f.estadoFiscal().equals(d.getEstadoFiscal())) continue;
            if (lleno(f.provincia()) && !f.provincia().equals(lugar.provincia())) continue;
            if (lleno(f.distrito()) && !f.distrito().equals(lugar.distrito())) continue;
            if (f.desde() != null && fecha.isBefore(f.desde())) continue;
            if (f.hasta() != null && fecha.isAfter(f.hasta())) continue;
            if (texto != null && !(d.getCodigo() + " " + ciudadano + " " + d.getCiudadano().getDni() + " " + d.getLugarHechos()
                    + " " + d.getCarpetaFiscal() + " " + d.getDescripcion()).toLowerCase(Locale.ROOT).contains(texto)) continue;
            InactividadService.Inactividad i = inactividad.getOrDefault(d.getCodigo(), new InactividadService.Inactividad(
                    d.getFechaRegistro(), "Registro de la denuncia", 0, InactividadService.NORMAL));
            if (!cumple(f.situacion(), i)) continue;
            filas.add(new ReporteFilaDTO(d.getCodigo(), d.getFechaRegistro(), d.getTipoDelito(), ciudadano, d.getCiudadano().getDni(),
                    d.getLugarHechos(), lugar.distrito(), lugar.provincia(), d.getEstadoPolicial(), d.getEstadoFiscal(),
                    d.getCarpetaFiscal(), d.getFiscalResponsable(), d.getDescripcion(),
                    i.ultimaActividad(), i.tipoActividad(), i.dias(), i.situacion()));
        }
        // Al filtrar por inactividad, primero las que llevan más días sin actividad
        if (lleno(f.situacion())) {
            filas.sort((a, b) -> Long.compare(b.diasSinActividad(), a.diasSinActividad()));
        } else {
            filas.sort((a, b) -> b.fechaRegistro().compareTo(a.fechaRegistro()));
        }
        return filas;
    }

    /** Tablero de denuncias sin actividad: cantidades por situación, por delito, por responsable y las más inactivas. */
    public InactividadDTO inactividad() {
        Map<String, InactividadService.Inactividad> inactividad = inactividadService.todas();
        // Policía responsable de cada denuncia: código -> [código del policía, nombre]
        Map<String, String[]> responsables = new HashMap<>();
        jdbc.query("SELECT d.codigo, u.codigo, u.nombres || ' ' || u.apellidos FROM denuncia d "
                + "LEFT JOIN usuario u ON u.id_usuario = d.id_usuario_responsable",
                rs -> { responsables.put(rs.getString(1), new String[]{rs.getString(2), rs.getString(3)}); });

        Map<String, Integer> porSituacion = new LinkedHashMap<>();
        InactividadService.SITUACIONES.forEach(x -> porSituacion.put(x, 0));
        Map<String, int[]> porDelito = new TreeMap<>();          // [normal, seguimiento, crítica, prioritaria]
        Map<String, int[]> porResponsable = new HashMap<>();     // [crítica, prioritaria]
        List<InactividadDTO.Fila> activas = new ArrayList<>();
        long sumaDias = 0;
        for (Denuncia d : denunciaRepository.listarConCiudadano()) {
            InactividadService.Inactividad i = inactividad.get(d.getCodigo());
            if (i == null) continue;
            porSituacion.merge(i.situacion(), 1, Integer::sum);
            if (InactividadService.CONCLUIDA.equals(i.situacion())) continue;
            sumaDias += i.dias();
            int columna = InactividadService.SITUACIONES.indexOf(i.situacion());
            porDelito.computeIfAbsent(d.getTipoDelito(), k -> new int[4])[columna]++;
            String[] responsable = responsables.getOrDefault(d.getCodigo(), new String[]{null, null});
            if (i.dias() >= InactividadService.DIAS_POR_VENCER && responsable[0] != null) {
                porResponsable.computeIfAbsent(responsable[0] + "|" + responsable[1], k -> new int[2])
                        [InactividadService.CRITICA.equals(i.situacion()) ? 0 : 1]++;
            }
            activas.add(new InactividadDTO.Fila(d.getCodigo(), d.getTipoDelito(), d.getCiudadano().getNombreCompleto(),
                    responsable[1] == null ? "Sin asignar" : responsable[0] + " · " + responsable[1],
                    i.ultimaActividad(), i.tipoActividad(), i.dias(), i.situacion()));
        }
        List<InactividadDTO.Categoria> categorias = porSituacion.entrySet().stream()
                .map(e -> new InactividadDTO.Categoria(e.getKey(), InactividadService.rango(e.getKey()), e.getValue())).toList();
        List<InactividadDTO.PorDelito> delitos = porDelito.entrySet().stream()
                .map(e -> new InactividadDTO.PorDelito(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3]))
                .sorted((a, b) -> Integer.compare(b.critica() + b.prioritaria(), a.critica() + a.prioritaria()))
                .toList();
        List<InactividadDTO.PorResponsable> responsablesTop = porResponsable.entrySet().stream()
                .map(e -> { String[] p = e.getKey().split("\\|", 2);
                    return new InactividadDTO.PorResponsable(p[0], p[1], e.getValue()[0], e.getValue()[1]); })
                .sorted((a, b) -> Integer.compare(b.critica() + b.prioritaria(), a.critica() + a.prioritaria()))
                .limit(8).toList();
        activas.sort((a, b) -> Long.compare(b.dias(), a.dias()));
        int porVencer = (int) activas.stream().filter(f -> f.dias() >= InactividadService.DIAS_POR_VENCER).count();
        return new InactividadDTO(activas.size(), porVencer, activas.isEmpty() ? 0 : Math.round((double) sumaDias / activas.size()),
                categorias, delitos, responsablesTop, activas.stream().limit(10).toList());
    }

    /** Reporte en PDF (hoja horizontal). */
    public byte[] pdf(List<ReporteFilaDTO> filas, Filtro filtro, String funcionario) {
        PdfSimple pdf = new PdfSimple(true);
        pdf.titulo("Reporte institucional de denuncias");
        pdf.dato("Funcionario", funcionario);
        pdf.dato("Filtros", filtro.descripcion());
        pdf.dato("Fecha de emisión", LocalDateTime.now().format(FECHA_HORA));
        pdf.dato("Total de denuncias", String.valueOf(filas.size()));
        pdf.separador();
        List<String[]> datos = filas.stream().map(r -> new String[]{
                r.codigo(), r.fechaRegistro().format(FECHA), r.ultimaActividad().format(FECHA), String.valueOf(r.diasSinActividad()),
                r.situacion(), r.tipoDelito(), r.ciudadano(), r.distrito(), r.estadoPolicial(), r.estadoFiscal(),
                r.carpetaFiscal() == null ? "-" : r.carpetaFiscal()}).toList();
        if (datos.isEmpty()) {
            pdf.parrafo("No se encontraron denuncias con los filtros seleccionados.", 10);
        } else {
            pdf.tabla(new String[]{"Código", "Registro", "Últ. actividad", "Días", "Situación", "Delito", "Ciudadano", "Distrito", "Estado PNP", "Estado Fiscalía", "Carpeta fiscal"},
                    new float[]{78, 50, 56, 28, 60, 92, 110, 74, 58, 66, 90}, datos);
        }
        return pdf.generar("Sistema de Gestión y Seguimiento de Denuncias · PNP - Fiscalía · Prototipo académico con datos ficticios");
    }

    /** Reporte en Excel (.xlsx) con filtros en la fila de encabezados. */
    public byte[] excel(List<ReporteFilaDTO> filas, Filtro filtro, String funcionario) {
        XlsxSimple excel = new XlsxSimple("Denuncias");
        excel.fila(XlsxSimple.TITULO, "Reporte institucional de denuncias");
        excel.fila(XlsxSimple.ETIQUETA, "Funcionario", funcionario);
        excel.fila(XlsxSimple.ETIQUETA, "Filtros", filtro.descripcion());
        excel.fila(XlsxSimple.ETIQUETA, "Fecha de emisión", LocalDateTime.now().format(FECHA_HORA));
        excel.fila(XlsxSimple.ETIQUETA, "Total de denuncias", String.valueOf(filas.size()));
        excel.filaVacia();
        excel.fila(XlsxSimple.ENCABEZADO, "Código", "Fecha de registro", "Última actividad", "Actividad", "Días sin actividad", "Situación", "Tipo de delito", "Ciudadano", "DNI", "Lugar de los hechos",
                "Distrito", "Provincia", "Estado PNP", "Estado Fiscalía", "Carpeta fiscal", "Fiscal responsable", "Descripción");
        for (ReporteFilaDTO r : filas) {
            excel.fila(XlsxSimple.CELDA, r.codigo(), r.fechaRegistro().format(FECHA_HORA), r.ultimaActividad().format(FECHA),
                    r.tipoActividad(), String.valueOf(r.diasSinActividad()), r.situacion(), r.tipoDelito(), r.ciudadano(), r.dni(),
                    r.lugarHechos(), r.distrito(), r.provincia(), r.estadoPolicial(), r.estadoFiscal(), r.carpetaFiscal(),
                    r.fiscalResponsable(), r.descripcion());
        }
        excel.anchos(18, 17, 15, 26, 11, 13, 26, 32, 11, 42, 20, 16, 15, 17, 16, 30, 60);
        return excel.generar();
    }

    private static boolean lleno(String valor) {
        return valor != null && !valor.isBlank();
    }
}
