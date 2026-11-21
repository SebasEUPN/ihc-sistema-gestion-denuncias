package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.ActualizarDenunciaRequest;
import com.proyecto.denuncias.dto.DenunciaDTO;
import com.proyecto.denuncias.dto.SeguimientoDTO;
import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Denuncia;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.SeguimientoDenuncia;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.repository.SeguimientoDenunciaRepository;
import com.proyecto.denuncias.security.Sesion;
import com.proyecto.denuncias.util.Validar;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio de las denuncias (carpetas fiscales para el fiscal).
 */
@Service
public class DenunciaService {

    /** Estados que puede asignar cada perfil al actualizar una denuncia. */
    private static final List<String> ESTADOS_POLICIA = List.of("En proceso", "Por resolver", "Resuelto");
    private static final List<String> ESTADOS_FISCAL = List.of("En investigación", "Diligencias", "Con disposición", "Archivado");

    private final DenunciaRepository denunciaRepository;
    private final SeguimientoDenunciaRepository seguimientoRepository;
    private final AuditoriaService auditoriaService;

    public DenunciaService(DenunciaRepository denunciaRepository,
                           SeguimientoDenunciaRepository seguimientoRepository,
                           AuditoriaService auditoriaService) {
        this.denunciaRepository = denunciaRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.auditoriaService = auditoriaService;
    }

    /** Busca la entidad por su código o responde 404. */
    private Denuncia buscar(String codigo) {
        return denunciaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe la denuncia " + codigo + "."));
    }

    /** Lista todas las denuncias con el estado que corresponde al perfil que consulta. */
    @Transactional(readOnly = true)
    public List<DenunciaDTO> listar(Rol rol) {
        boolean vistaFiscal = rol == Rol.FISCAL;
        return denunciaRepository.listarConCiudadano().stream()
                .map(d -> DenunciaDTO.de(d, vistaFiscal, List.of()))
                .toList();
    }

    /** Detalle de una denuncia incluyendo su historial de seguimiento. */
    @Transactional(readOnly = true)
    public DenunciaDTO obtener(String codigo, Rol rol) {
        Denuncia denuncia = buscar(codigo);
        List<SeguimientoDTO> historial = seguimientoRepository
                .findByDenunciaIdOrderByFechaHoraDesc(denuncia.getId()).stream()
                .map(SeguimientoDTO::de)
                .toList();
        return DenunciaDTO.de(denuncia, rol == Rol.FISCAL, historial);
    }

    /**
     * Actualiza el estado de una denuncia y agrega una observación al historial.
     * El policía modifica el estado policial y el fiscal el estado fiscal.
     */
    @Transactional
    public DenunciaDTO actualizar(String codigo, ActualizarDenunciaRequest datos, Sesion.Datos sesion) {
        boolean esFiscal = sesion.rol() == Rol.FISCAL;
        String estado = Validar.texto(datos.estado(), "Estado", 30);
        // Todo cambio de estado debe justificarse: es una actuación válida y reinicia el contador de inactividad
        String comentario = Validar.opcional(datos.comentario(), "Justificación", 1000);
        if (comentario == null || comentario.length() < 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Escribe la justificación de la actuación (al menos 10 caracteres) para registrar el cambio.");
        }

        // Cada perfil solo puede usar los estados de su institución
        List<String> permitidos = esFiscal ? ESTADOS_FISCAL : ESTADOS_POLICIA;
        if (!permitidos.contains(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El estado \"" + estado + "\" no es válido para tu perfil.");
        }

        Denuncia denuncia = buscar(codigo);
        LocalDateTime ahora = LocalDateTime.now();
        if (esFiscal) {
            denuncia.setEstadoFiscal(estado);
        } else {
            denuncia.setEstadoPolicial(estado);
        }
        denuncia.setUltimaActualizacion(ahora);

        // Se deja constancia del cambio en el historial y en la auditoría
        seguimientoRepository.save(new SeguimientoDenuncia(denuncia, sesion.codigo(), sesion.rol(), estado, comentario, ahora));
        auditoriaService.registrar(sesion.codigo(),
                (esFiscal ? "Registró disposición en " : "Actualizó la denuncia ") + codigo + " (" + estado + ")",
                esFiscal ? "Fiscalía" : "Denuncias");

        return obtener(codigo, sesion.rol());
    }

    /** Denuncias registradas entre dos fechas (ambas incluidas) para el módulo de reportes. */
    @Transactional
    public List<DenunciaDTO> reporte(LocalDate desde, LocalDate hasta, Sesion.Datos sesion) {
        if (desde == null || hasta == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona ambas fechas para consultar.");
        }
        if (desde.isAfter(hasta)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }
        boolean vistaFiscal = sesion.rol() == Rol.FISCAL;
        // Desde las 00:00 del primer día hasta las 23:59:59 del último
        List<DenunciaDTO> resultado = denunciaRepository
                .findByFechaRegistroBetweenOrderByCodigoAsc(desde.atStartOfDay(), hasta.atTime(23, 59, 59)).stream()
                .map(d -> DenunciaDTO.de(d, vistaFiscal, List.of()))
                .toList();
        auditoriaService.registrar(sesion.codigo(), "Consultó reporte de denuncias del " + desde + " al " + hasta, "Reportes");
        return resultado;
    }
}
