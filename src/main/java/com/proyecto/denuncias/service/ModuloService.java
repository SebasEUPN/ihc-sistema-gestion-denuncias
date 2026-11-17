package com.proyecto.denuncias.service;

import com.proyecto.denuncias.model.ActividadProgramada;
import com.proyecto.denuncias.model.Actuacion;
import com.proyecto.denuncias.model.CopiaSeguridad;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.repository.ActividadProgramadaRepository;
import com.proyecto.denuncias.repository.ActuacionRepository;
import com.proyecto.denuncias.repository.CopiaSeguridadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de los módulos de apoyo: actuaciones, agenda y copias de seguridad.
 * (Documentos: DocumentoService. Remisiones, disposiciones y expediente fiscal: ExpedienteService.)
 */
@Service
public class ModuloService {

    private final ActuacionRepository actuacionRepository;
    private final ActividadProgramadaRepository actividadRepository;
    private final CopiaSeguridadRepository copiaRepository;
    private final AuditoriaService auditoriaService;

    public ModuloService(ActuacionRepository actuacionRepository, ActividadProgramadaRepository actividadRepository,
                         CopiaSeguridadRepository copiaRepository, AuditoriaService auditoriaService) {
        this.actuacionRepository = actuacionRepository;
        this.actividadRepository = actividadRepository;
        this.copiaRepository = copiaRepository;
        this.auditoriaService = auditoriaService;
    }

    /** Actuaciones del perfil: policiales para el policía y fiscales para el fiscal. */
    @Transactional(readOnly = true)
    public List<Actuacion> actuaciones(Rol rol) {
        return actuacionRepository.findByPerfilOrderByCodigoAsc(rol);
    }

    /** Agenda del perfil (programación policial o diligencias fiscales). */
    @Transactional(readOnly = true)
    public List<ActividadProgramada> agenda(Rol rol) {
        return rol == Rol.FISCAL
                ? actividadRepository.findByIdFiscalIsNotNullOrderByFechaHoraAsc()
                : actividadRepository.findByIdPoliciaIsNotNullOrderByFechaHoraAsc();
    }
}
