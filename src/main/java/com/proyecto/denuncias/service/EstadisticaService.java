package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.EstadisticasDTO;
import com.proyecto.denuncias.dto.EstadisticasDTO.Conteo;
import com.proyecto.denuncias.dto.EstadisticasDTO.EnLinea;
import com.proyecto.denuncias.dto.EstadisticasDTO.FilaFiscal;
import com.proyecto.denuncias.dto.EstadisticasDTO.FilaPolicia;
import com.proyecto.denuncias.dto.EstadisticasDTO.FilaUsuario;
import com.proyecto.denuncias.dto.EstadisticasDTO.Resumen;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.repository.ActuacionRepository;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.repository.DocumentoRepository;
import com.proyecto.denuncias.repository.MensajeRepository;
import com.proyecto.denuncias.repository.RegistroActividadRepository;
import com.proyecto.denuncias.repository.RemisionFiscalRepository;
import com.proyecto.denuncias.repository.SeguimientoDenunciaRepository;
import com.proyecto.denuncias.repository.UsuarioRepository;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.security.UsuariosActivos;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula el tablero de estadísticas del administrador: indicadores generales,
 * gráficos y desempeño de los funcionarios policiales y fiscales.
 *
 * Los conteos los hace SQL Server con consultas "GROUP BY" (ver los repositorios);
 * aquí solo se ordenan y se arman las filas que necesita la pantalla.
 */
@Service
public class EstadisticaService {

    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};

    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SeguimientoDenunciaRepository seguimientoRepository;
    private final RegistroActividadRepository registroRepository;
    private final MensajeRepository mensajeRepository;
    private final ActuacionRepository actuacionRepository;
    private final DocumentoRepository documentoRepository;
    private final RemisionFiscalRepository remisionRepository;
    private final UsuariosActivos usuariosActivos;

    public EstadisticaService(DenunciaRepository denunciaRepository, UsuarioRepository usuarioRepository,
                              SeguimientoDenunciaRepository seguimientoRepository,
                              RegistroActividadRepository registroRepository, MensajeRepository mensajeRepository,
                              ActuacionRepository actuacionRepository, DocumentoRepository documentoRepository,
                              RemisionFiscalRepository remisionRepository, UsuariosActivos usuariosActivos) {
        this.denunciaRepository = denunciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.registroRepository = registroRepository;
        this.mensajeRepository = mensajeRepository;
        this.actuacionRepository = actuacionRepository;
        this.documentoRepository = documentoRepository;
        this.remisionRepository = remisionRepository;
        this.usuariosActivos = usuariosActivos;
    }

    /** Arma todas las estadísticas en una sola respuesta. */
    public EstadisticasDTO calcular() {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByIdAsc();

        // ---------- Personas usando el sistema en este momento ----------
        List<Sesion.Datos> activos = usuariosActivos.activos();
        Set<Long> idsEnLinea = activos.stream().map(Sesion.Datos::id).collect(Collectors.toSet());
        EnLinea enLinea = new EnLinea(activos.size(),
                activos.stream().filter(a -> a.rol() == Rol.POLICIA).count(),
                activos.stream().filter(a -> a.rol() == Rol.FISCAL).count(),
                activos.stream().filter(a -> a.rol() == Rol.ADMIN).count());

        // ---------- Conteos generales de denuncias ----------
        List<Conteo> estadoPolicial = conteos(denunciaRepository.contarPorEstadoPolicial());
        List<Conteo> estadoFiscal = conteos(denunciaRepository.contarPorEstadoFiscal());
        List<Conteo> porDelito = conteos(denunciaRepository.contarPorTipoDelito());
        long total = estadoPolicial.stream().mapToLong(Conteo::cantidad).sum();
        long resueltas = cantidad(estadoPolicial, "Resuelto");
        long conDecision = cantidad(estadoFiscal, "Con disposición") + cantidad(estadoFiscal, "Archivado");

        // ---------- Denuncias por mes (últimos 12 meses), hoy y este mes ----------
        List<LocalDateTime> fechas = denunciaRepository.fechasDeRegistro();
        Map<YearMonth, Long> cantidadPorMes = fechas.stream()
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
        YearMonth mesActual = YearMonth.now();
        List<Conteo> porMes = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth mes = mesActual.minusMonths(i);
            String etiqueta = MESES[mes.getMonthValue() - 1] + " " + String.valueOf(mes.getYear()).substring(2);
            porMes.add(new Conteo(etiqueta, cantidadPorMes.getOrDefault(mes, 0L)));
        }
        LocalDate hoy = LocalDate.now();
        long registradasHoy = fechas.stream().filter(f -> f.toLocalDate().equals(hoy)).count();

        // ---------- Tiempo promedio de atención (días entre el registro y la resolución) ----------
        double diasPromedio = denunciaRepository.periodosDeResueltas().stream()
                .mapToDouble(p -> Duration.between((LocalDateTime) p[0], (LocalDateTime) p[1]).toHours() / 24.0)
                .average().orElse(0);

        Resumen resumen = new Resumen(total, registradasHoy, cantidadPorMes.getOrDefault(mesActual, 0L),
                resueltas, porcentaje(resueltas, total), conDecision, porcentaje(conDecision, total),
                redondear(diasPromedio),
                usuarios.stream().filter(u -> u.getRol() != Rol.ADMIN && u.isHabilitado()).count(),
                usuarios.stream().filter(u -> u.getRol() != Rol.ADMIN && !u.isHabilitado()).count());

        // ---------- Actuaciones (por perfil), documentos y remisiones ----------
        List<Conteo> actuacionesPoliciales = new ArrayList<>();
        List<Conteo> actuacionesFiscales = new ArrayList<>();
        for (Object[] fila : actuacionRepository.contarPorPerfilYEstado()) {
            Conteo conteo = new Conteo((String) fila[1], numero(fila[2]));
            (fila[0] == Rol.FISCAL ? actuacionesFiscales : actuacionesPoliciales).add(conteo);
        }

        return new EstadisticasDTO(enLinea, resumen, porMes, estadoPolicial, estadoFiscal, porDelito,
                actuacionesPoliciales, actuacionesFiscales,
                conteos(documentoRepository.contarPorEstado()), conteos(remisionRepository.contarPorEstado()),
                filasPolicias(usuarios, idsEnLinea), filasFiscales(), filasActividad(usuarios, idsEnLinea));
    }

    // ------------------------------------------------------------------
    // Tablas de desempeño
    // ------------------------------------------------------------------

    /** Denuncias a cargo de cada funcionario policial, separadas por estado. */
    private List<FilaPolicia> filasPolicias(List<Usuario> usuarios, Set<Long> idsEnLinea) {
        // idUsuario -> (estado -> cantidad)
        Map<Long, Map<String, Long>> porResponsable = new HashMap<>();
        try {
            for (Object[] fila : denunciaRepository.contarPorResponsableYEstado()) {
                porResponsable.computeIfAbsent(numero(fila[0]), k -> new HashMap<>()).put((String) fila[1], numero(fila[2]));
            }
        } catch (RuntimeException e) {
            // La columna id_usuario_responsable aún no existe (falta ejecutar el script 02):
            // la tabla se muestra con todos los funcionarios en cero en lugar de fallar.
        }

        List<FilaPolicia> filas = new ArrayList<>();
        for (Usuario u : usuarios) {
            if (u.getRol() != Rol.POLICIA) {
                continue;
            }
            Map<String, Long> estados = porResponsable.getOrDefault(u.getId(), Map.of());
            long asignadas = estados.values().stream().mapToLong(Long::longValue).sum();
            long resueltas = estados.getOrDefault("Resuelto", 0L);
            filas.add(new FilaPolicia(u.getCodigo(), u.getNombreCompleto(), asignadas,
                    estados.getOrDefault("En proceso", 0L), estados.getOrDefault("Por resolver", 0L),
                    resueltas, porcentaje(resueltas, asignadas), idsEnLinea.contains(u.getId())));
        }
        // Primero los que tienen más denuncias asignadas
        filas.sort(Comparator.comparingLong(FilaPolicia::asignadas).reversed());
        return filas;
    }

    /** Carpetas a cargo de cada fiscal responsable, separadas por estado. */
    private List<FilaFiscal> filasFiscales() {
        // nombre del fiscal -> (estado -> cantidad)
        Map<String, Map<String, Long>> porFiscal = new HashMap<>();
        for (Object[] fila : denunciaRepository.contarPorFiscalYEstado()) {
            porFiscal.computeIfAbsent((String) fila[0], k -> new HashMap<>()).put((String) fila[1], numero(fila[2]));
        }
        List<FilaFiscal> filas = new ArrayList<>();
        porFiscal.forEach((nombre, estados) -> filas.add(new FilaFiscal(nombre,
                estados.values().stream().mapToLong(Long::longValue).sum(),
                estados.getOrDefault("En investigación", 0L), estados.getOrDefault("Diligencias", 0L),
                estados.getOrDefault("Con disposición", 0L), estados.getOrDefault("Archivado", 0L))));
        filas.sort(Comparator.comparingLong(FilaFiscal::carpetas).reversed());
        return filas;
    }

    /** Uso del sistema por cada funcionario: actualizaciones, operaciones y mensajes. */
    private List<FilaUsuario> filasActividad(List<Usuario> usuarios, Set<Long> idsEnLinea) {
        Map<String, Long> actualizaciones = mapa(seguimientoRepository.contarPorUsuario());
        Map<String, Long> operaciones = mapa(registroRepository.contarPorUsuario());
        Map<Long, Long> mensajes = new HashMap<>();
        for (Object[] fila : mensajeRepository.contarPorRemitente()) {
            mensajes.put(numero(fila[0]), numero(fila[1]));
        }

        List<FilaUsuario> filas = new ArrayList<>();
        for (Usuario u : usuarios) {
            if (u.getRol() == Rol.ADMIN) {
                continue;
            }
            filas.add(new FilaUsuario(u.getCodigo(), u.getNombreCompleto(), u.getRol().getEtiqueta(),
                    actualizaciones.getOrDefault(u.getCodigo(), 0L), operaciones.getOrDefault(u.getCodigo(), 0L),
                    mensajes.getOrDefault(u.getId(), 0L), u.getUltimoAcceso(), idsEnLinea.contains(u.getId())));
        }
        // Primero los conectados y, después, los que más han usado el sistema
        filas.sort(Comparator.comparing(FilaUsuario::enLinea).reversed()
                .thenComparing(Comparator.comparingLong((FilaUsuario f) -> f.actualizaciones() + f.operaciones() + f.mensajes()).reversed()));
        return filas;
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------

    /** Convierte las filas [nombre, cantidad] de una consulta en una lista ordenada de mayor a menor. */
    private List<Conteo> conteos(List<Object[]> filas) {
        return filas.stream()
                .map(f -> new Conteo(String.valueOf(f[0]), numero(f[1])))
                .sorted(Comparator.comparingLong(Conteo::cantidad).reversed())
                .toList();
    }

    /** Convierte las filas [código, cantidad] en un mapa para buscar por código. */
    private Map<String, Long> mapa(List<Object[]> filas) {
        Map<String, Long> resultado = new HashMap<>();
        for (Object[] fila : filas) {
            resultado.put((String) fila[0], numero(fila[1]));
        }
        return resultado;
    }

    /** Cantidad que corresponde a un nombre dentro de una lista de conteos (0 si no está). */
    private long cantidad(List<Conteo> lista, String nombre) {
        return lista.stream().filter(c -> c.nombre().equals(nombre)).mapToLong(Conteo::cantidad).sum();
    }

    /** Las consultas devuelven los números como Long, Integer o BigInteger según el caso. */
    private long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    /** Porcentaje con un decimal (0 si el total es cero). */
    private double porcentaje(long parte, long total) {
        return total == 0 ? 0 : redondear(parte * 100.0 / total);
    }

    private double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
