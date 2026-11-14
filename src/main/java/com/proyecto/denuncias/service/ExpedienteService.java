package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.DisposicionDTO;
import com.proyecto.denuncias.dto.ExpedienteDTO;
import com.proyecto.denuncias.dto.ExpedienteDTO.ActuacionItem;
import com.proyecto.denuncias.dto.ExpedienteDTO.DisposicionItem;
import com.proyecto.denuncias.dto.ExpedienteDTO.Etapa;
import com.proyecto.denuncias.dto.RemisionDTO;
import com.proyecto.denuncias.dto.SeguimientoDTO;
import com.proyecto.denuncias.dto.SeguimientoFiscalDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Actuacion;
import com.proyecto.denuncias.model.Ciudadano;
import com.proyecto.denuncias.model.Denuncia;
import com.proyecto.denuncias.model.DisposicionFiscal;
import com.proyecto.denuncias.model.RemisionFiscal;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.repository.ActuacionRepository;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.repository.DisposicionFiscalRepository;
import com.proyecto.denuncias.repository.RemisionFiscalRepository;
import com.proyecto.denuncias.repository.SeguimientoDenunciaRepository;
import com.proyecto.denuncias.util.Ubicacion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Información fiscal de las denuncias: expediente completo, etapa de la
 * investigación, remisiones de la PNP y disposiciones de la Fiscalía.
 *
 * Los métodos no usan @Transactional a propósito: cada consulta al repositorio
 * es independiente, así una consulta que falle (por ejemplo, si falta la
 * columna del funcionario responsable) no anula las demás.
 */
@Service
public class ExpedienteService {

    /** Nombres de las etapas de la investigación fiscal, en orden. */
    private static final String[] ETAPAS = {
            "Remisión a la Fiscalía", "Carpeta fiscal asignada", "Investigación preliminar",
            "Diligencias fiscales", "Decisión fiscal"};

    private final DenunciaRepository denunciaRepository;
    private final RemisionFiscalRepository remisionRepository;
    private final DisposicionFiscalRepository disposicionRepository;
    private final ActuacionRepository actuacionRepository;
    private final SeguimientoDenunciaRepository seguimientoRepository;
    private final DocumentoService documentoService;

    public ExpedienteService(DenunciaRepository denunciaRepository, RemisionFiscalRepository remisionRepository,
                             DisposicionFiscalRepository disposicionRepository, ActuacionRepository actuacionRepository,
                             SeguimientoDenunciaRepository seguimientoRepository, DocumentoService documentoService) {
        this.denunciaRepository = denunciaRepository;
        this.remisionRepository = remisionRepository;
        this.disposicionRepository = disposicionRepository;
        this.actuacionRepository = actuacionRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.documentoService = documentoService;
    }

    /** Expediente completo de una denuncia. */
    public ExpedienteDTO expediente(String codigo) {
        Denuncia d = denunciaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe la denuncia " + codigo + "."));
        RemisionFiscal remision = remisionRepository.findFirstByCodigoDenunciaOrderByFechaRemisionDescIdDesc(d.getCodigo()).orElse(null);
        List<DisposicionFiscal> disposiciones = d.getCarpetaFiscal() == null ? List.of()
                : disposicionRepository.findByCarpetaFiscalOrderByFechaAscIdAsc(d.getCarpetaFiscal());
        List<Actuacion> actuaciones = actuacionRepository.findByCodigoDenunciaOrderByFechaAscIdAsc(d.getCodigo());
        long actuacionesFiscales = actuaciones.stream().filter(a -> a.getPerfil() == Rol.FISCAL).count();
        LocalDate fechaDecision = disposiciones.isEmpty() ? null : disposiciones.get(disposiciones.size() - 1).getFecha();

        List<Etapa> etapas = etapas(d, remision, fechaDecision, actuacionesFiscales,
                disposiciones.isEmpty() ? null : disposiciones.get(disposiciones.size() - 1).getTipo());
        Ciudadano c = d.getCiudadano();
        Ubicacion.Partes lugar = Ubicacion.de(d.getLugarHechos());

        return new ExpedienteDTO(
                d.getCodigo(), d.getTipoDelito(), d.getDescripcion(), d.getLugarHechos(), lugar.distrito(), lugar.provincia(),
                d.getFechaRegistro(), d.getUltimaActualizacion(), d.getEstadoPolicial(), d.getEstadoFiscal(),
                d.getCarpetaFiscal(), d.getDespachoFiscal(), d.getFiscalResponsable(),
                new ExpedienteDTO.Persona(c.getNombreCompleto(), c.getDni(), c.getTelefono(), c.getCorreo()),
                responsablePolicial(d.getCodigo()),
                remision == null ? null : new ExpedienteDTO.Remision(remision.getFechaRemision(), remision.getUltimaRespuesta(), remision.getEstado()),
                remision != null && "Requerimiento".equals(remision.getEstado()),
                etapas, etapaActual(etapas), avance(etapas),
                disposiciones.stream().map(x -> new DisposicionItem(x.getNumero(), x.getTipo(), x.getFecha(), x.getFiscalResponsable(), x.getEstado())).toList(),
                actuaciones.stream().map(a -> new ActuacionItem(a.getCodigo(), a.getPerfil().getEtiqueta(), a.getDescripcion(), a.getFecha(), a.getEstado())).toList(),
                documentoService.deDenuncia(d.getCodigo()),
                seguimientoRepository.findByDenunciaIdOrderByFechaHoraDesc(d.getId()).stream().map(SeguimientoDTO::de).toList());
    }

    /** "Seguimiento de Fiscalía": etapa en la que está cada denuncia con carpeta fiscal. */
    public List<SeguimientoFiscalDTO> seguimiento() {
        Map<String, RemisionFiscal> remisiones = ultimaRemisionPorDenuncia();
        List<SeguimientoFiscalDTO> filas = new ArrayList<>();
        for (Denuncia d : denunciaRepository.listarConCiudadano()) {
            RemisionFiscal r = remisiones.get(d.getCodigo());
            List<Etapa> etapas = etapas(d, r, null, 0, null);
            int actual = etapaActual(etapas);
            filas.add(new SeguimientoFiscalDTO(d.getCodigo(), d.getTipoDelito(), d.getCiudadano().getNombreCompleto(),
                    d.getCarpetaFiscal(), d.getDespachoFiscal(), d.getFiscalResponsable(), d.getEstadoFiscal(),
                    etapas.get(actual).titulo(), actual + 1, etapas.size(), avance(etapas),
                    r == null ? null : r.getEstado(), r == null ? null : r.getFechaRemision(),
                    r != null && "Requerimiento".equals(r.getEstado()), d.getUltimaActualizacion()));
        }
        return filas;
    }

    /** "Informes a Fiscalía": remisiones de la PNP con lo que responde la Fiscalía. */
    public List<RemisionDTO> remisiones() {
        List<RemisionFiscal> lista = remisionRepository.findAllByOrderByFechaRemisionAscIdAsc();
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<String, Denuncia> denuncias = denunciaRepository.findByCodigoIn(lista.stream().map(RemisionFiscal::getCodigoDenuncia).toList())
                .stream().collect(Collectors.toMap(Denuncia::getCodigo, Function.identity()));
        Map<String, Long> disposicionesPorCarpeta = disposicionRepository.findAll().stream()
                .collect(Collectors.groupingBy(DisposicionFiscal::getCarpetaFiscal, Collectors.counting()));
        List<RemisionDTO> filas = new ArrayList<>();
        for (RemisionFiscal r : lista) {
            Denuncia d = denuncias.get(r.getCodigoDenuncia());
            if (d == null) continue;
            // La carpeta de la denuncia es la vigente; la de la remisión puede no haberse registrado aún
            String carpeta = d.getCarpetaFiscal() != null ? d.getCarpetaFiscal() : r.getCarpetaFiscal();
            List<Etapa> etapas = etapas(d, r, null, 0, null);
            filas.add(new RemisionDTO(d.getCodigo(), d.getTipoDelito(), d.getCiudadano().getNombreCompleto(), carpeta,
                    d.getDespachoFiscal(), d.getFiscalResponsable(), r.getFechaRemision(), r.getUltimaRespuesta(), r.getEstado(),
                    d.getEstadoFiscal(), etapas.get(etapaActual(etapas)).titulo(),
                    carpeta == null ? 0 : disposicionesPorCarpeta.getOrDefault(carpeta, 0L)));
        }
        filas.sort(Comparator.comparing(RemisionDTO::fechaRemision).reversed());
        return filas;
    }

    /** Disposiciones fiscales con la denuncia a la que pertenece cada carpeta. */
    public List<DisposicionDTO> disposiciones() {
        List<DisposicionFiscal> lista = disposicionRepository.findAllByOrderByNumeroAsc();
        Map<String, Denuncia> porCarpeta = new HashMap<>();
        for (Denuncia d : denunciaRepository.listarConCiudadano()) {
            if (d.getCarpetaFiscal() != null) porCarpeta.putIfAbsent(d.getCarpetaFiscal(), d);
        }
        return lista.stream().map(x -> {
            Denuncia d = porCarpeta.get(x.getCarpetaFiscal());
            return new DisposicionDTO(x.getNumero(), x.getCarpetaFiscal(), x.getTipo(), x.getFecha(), x.getFiscalResponsable(),
                    x.getEstado(), d == null ? null : d.getCodigo(), d == null ? null : d.getTipoDelito(),
                    d == null ? null : d.getCiudadano().getNombreCompleto());
        }).toList();
    }

    // ------------------------------------------------------------------
    // Etapas de la investigación fiscal
    // ------------------------------------------------------------------

    /**
     * Calcula las 5 etapas a partir de lo registrado:
     *  1. Remisión a la Fiscalía      (hay remisión o la denuncia ya tiene carpeta)
     *  2. Carpeta fiscal asignada     (la denuncia tiene número de carpeta)
     *  3. Investigación preliminar    (estado fiscal "En investigación")
     *  4. Diligencias fiscales        (estado fiscal "Diligencias")
     *  5. Decisión fiscal             (estado fiscal "Con disposición" o "Archivado")
     */
    private List<Etapa> etapas(Denuncia d, RemisionFiscal r, LocalDate fechaDecision, long actuacionesFiscales, String ultimaDisposicion) {
        String estado = d.getEstadoFiscal() == null ? "" : d.getEstadoFiscal();
        boolean carpeta = d.getCarpetaFiscal() != null && !d.getCarpetaFiscal().isBlank();
        boolean decision = estado.equals("Con disposición") || estado.equals("Archivado");
        boolean diligencias = estado.equals("Diligencias");
        boolean investigacion = estado.equals("En investigación");
        List<Etapa> etapas = new ArrayList<>();

        // 1. Remisión
        if (r != null) {
            etapas.add(new Etapa(ETAPAS[0], "COMPLETADO", r.getFechaRemision(),
                    "Informe policial remitido. Estado de la remisión: " + r.getEstado() + "."));
        } else if (carpeta) {
            etapas.add(new Etapa(ETAPAS[0], "COMPLETADO", d.getFechaRegistro().toLocalDate(),
                    "La denuncia fue derivada a la Fiscalía; no se registró un informe de remisión aparte."));
        } else {
            etapas.add(new Etapa(ETAPAS[0], "PENDIENTE", null, "Aún no se registra la remisión a la Fiscalía."));
        }
        // 2. Carpeta fiscal
        etapas.add(carpeta
                ? new Etapa(ETAPAS[1], "COMPLETADO", r != null ? r.getUltimaRespuesta() : null,
                    "Carpeta " + d.getCarpetaFiscal() + texto(" · ", d.getDespachoFiscal()) + texto(" · ", d.getFiscalResponsable()) + ".")
                : new Etapa(ETAPAS[1], "EN_CURSO", null, "La Fiscalía aún no asigna número de carpeta."));
        // 3. Investigación preliminar
        etapas.add(new Etapa(ETAPAS[2], investigacion ? "EN_CURSO" : (diligencias || decision) ? "COMPLETADO" : "PENDIENTE", null,
                investigacion ? "La Fiscalía está reuniendo información sobre los hechos."
                        : (diligencias || decision) ? "Investigación preliminar concluida." : "Pendiente de iniciar."));
        // 4. Diligencias
        String detalleDiligencias = actuacionesFiscales > 0 ? " Actuaciones fiscales registradas: " + actuacionesFiscales + "." : "";
        etapas.add(new Etapa(ETAPAS[3], diligencias ? "EN_CURSO" : decision ? "COMPLETADO" : "PENDIENTE", null,
                (diligencias ? "Se están realizando diligencias (declaraciones, inspecciones, pericias)."
                        : decision ? "Diligencias concluidas." : "Pendiente.") + detalleDiligencias));
        // 5. Decisión fiscal
        etapas.add(new Etapa(ETAPAS[4], decision ? "COMPLETADO" : "PENDIENTE", decision ? fechaDecision : null,
                decision ? "Estado fiscal: " + estado + texto(". Última disposición: ", ultimaDisposicion) + "."
                        : "La Fiscalía aún no emite una decisión."));
        return etapas;
    }

    /** Posición de la primera etapa no completada (o la última si todas lo están). */
    private int etapaActual(List<Etapa> etapas) {
        for (int i = 0; i < etapas.size(); i++) {
            if (!etapas.get(i).estado().equals("COMPLETADO")) return i;
        }
        return etapas.size() - 1;
    }

    /** Porcentaje de avance: cada etapa completa suma 1 y la que está en curso suma media. */
    private int avance(List<Etapa> etapas) {
        double puntos = 0;
        for (Etapa e : etapas) {
            puntos += e.estado().equals("COMPLETADO") ? 1 : e.estado().equals("EN_CURSO") ? 0.5 : 0;
        }
        return (int) Math.round(puntos * 100 / etapas.size());
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------

    private Map<String, RemisionFiscal> ultimaRemisionPorDenuncia() {
        Map<String, RemisionFiscal> mapa = new HashMap<>();
        // Están ordenadas por fecha, así que la última que se guarda es la más reciente
        for (RemisionFiscal r : remisionRepository.findAllByOrderByFechaRemisionAscIdAsc()) {
            mapa.put(r.getCodigoDenuncia(), r);
        }
        return mapa;
    }

    /** Funcionario policial responsable (null si no hay o si falta la columna del script 02). */
    private ExpedienteDTO.Funcionario responsablePolicial(String codigo) {
        try {
            List<Object[]> filas = denunciaRepository.responsablePolicial(codigo);
            if (filas.isEmpty()) return null;
            Object[] f = filas.get(0);
            return new ExpedienteDTO.Funcionario((String) f[0], f[1] + " " + f[2], (String) f[3]);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String texto(String prefijo, String valor) {
        return valor == null || valor.isBlank() ? "" : prefijo + valor;
    }

    /** Normaliza un código escrito por el usuario. */
    public static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
