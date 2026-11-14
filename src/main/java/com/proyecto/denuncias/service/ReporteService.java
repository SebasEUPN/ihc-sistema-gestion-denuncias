package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.OpcionesReporteDTO;
import com.proyecto.denuncias.dto.ReporteFilaDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Denuncia;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.util.PdfSimple;
import com.proyecto.denuncias.util.Ubicacion;
import com.proyecto.denuncias.util.XlsxSimple;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
                         LocalDate desde, LocalDate hasta, String texto) {

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
            return partes.isEmpty() ? "Todas las denuncias (sin filtros)" : String.join(" · ", partes);
        }
    }

    private final DenunciaRepository denunciaRepository;

    public ReporteService(DenunciaRepository denunciaRepository) {
        this.denunciaRepository = denunciaRepository;
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
        String texto = lleno(f.texto()) ? f.texto().trim().toLowerCase(Locale.ROOT) : null;
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
            filas.add(new ReporteFilaDTO(d.getCodigo(), d.getFechaRegistro(), d.getTipoDelito(), ciudadano, d.getCiudadano().getDni(),
                    d.getLugarHechos(), lugar.distrito(), lugar.provincia(), d.getEstadoPolicial(), d.getEstadoFiscal(),
                    d.getCarpetaFiscal(), d.getFiscalResponsable(), d.getDescripcion()));
        }
        filas.sort((a, b) -> b.fechaRegistro().compareTo(a.fechaRegistro()));
        return filas;
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
                r.codigo(), r.fechaRegistro().format(FECHA), r.tipoDelito(), r.ciudadano(), r.distrito(), r.provincia(),
                r.estadoPolicial(), r.estadoFiscal(), r.carpetaFiscal() == null ? "-" : r.carpetaFiscal()}).toList();
        if (datos.isEmpty()) {
            pdf.parrafo("No se encontraron denuncias con los filtros seleccionados.", 10);
        } else {
            pdf.tabla(new String[]{"Código", "Fecha", "Delito", "Ciudadano", "Distrito", "Provincia", "Estado PNP", "Estado Fiscalía", "Carpeta fiscal"},
                    new float[]{82, 56, 112, 128, 88, 64, 64, 76, 92}, datos);
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
        excel.fila(XlsxSimple.ENCABEZADO, "Código", "Fecha de registro", "Tipo de delito", "Ciudadano", "DNI", "Lugar de los hechos",
                "Distrito", "Provincia", "Estado PNP", "Estado Fiscalía", "Carpeta fiscal", "Fiscal responsable", "Descripción");
        for (ReporteFilaDTO r : filas) {
            excel.fila(XlsxSimple.CELDA, r.codigo(), r.fechaRegistro().format(FECHA_HORA), r.tipoDelito(), r.ciudadano(), r.dni(),
                    r.lugarHechos(), r.distrito(), r.provincia(), r.estadoPolicial(), r.estadoFiscal(), r.carpetaFiscal(),
                    r.fiscalResponsable(), r.descripcion());
        }
        excel.anchos(18, 17, 26, 32, 11, 42, 20, 16, 15, 17, 16, 30, 60);
        return excel.generar();
    }

    private static boolean lleno(String valor) {
        return valor != null && !valor.isBlank();
    }
}
