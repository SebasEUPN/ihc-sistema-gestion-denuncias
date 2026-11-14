package com.proyecto.denuncias.service;

import com.proyecto.denuncias.dto.IndicadorDTO;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.repository.ActividadProgramadaRepository;
import com.proyecto.denuncias.repository.ActuacionRepository;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.repository.RegistroActividadRepository;
import com.proyecto.denuncias.repository.RemisionFiscalRepository;
import com.proyecto.denuncias.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Calcula las cuatro tarjetas de indicadores del panel principal.
 * Cada perfil ve indicadores distintos, obtenidos con consultas reales a la base de datos.
 */
@Service
public class PanelService {

    private final UsuarioRepository usuarioRepository;
    private final DenunciaRepository denunciaRepository;
    private final ActuacionRepository actuacionRepository;
    private final RemisionFiscalRepository remisionRepository;
    private final ActividadProgramadaRepository actividadRepository;
    private final RegistroActividadRepository registroRepository;

    public PanelService(UsuarioRepository usuarioRepository, DenunciaRepository denunciaRepository,
                        ActuacionRepository actuacionRepository, RemisionFiscalRepository remisionRepository,
                        ActividadProgramadaRepository actividadRepository,
                        RegistroActividadRepository registroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.denunciaRepository = denunciaRepository;
        this.actuacionRepository = actuacionRepository;
        this.remisionRepository = remisionRepository;
        this.actividadRepository = actividadRepository;
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public List<IndicadorDTO> indicadores(Rol rol) {
        long denuncias = denunciaRepository.count();

        if (rol == Rol.ADMIN) {
            return List.of(
                    new IndicadorDTO("Funcionarios", texto(usuarioRepository.countByRolNot(Rol.ADMIN)), "Cuentas institucionales"),
                    new IndicadorDTO("Cuentas activas", texto(usuarioRepository.countByRolNotAndHabilitadoTrue(Rol.ADMIN)), "Accesos habilitados"),
                    new IndicadorDTO("Denuncias registradas", texto(denuncias), "Registros en la base de datos"),
                    new IndicadorDTO("Reportes generados", texto(registroRepository.countByModulo("Reportes")), "Consultas registradas"));
        }
        if (rol == Rol.FISCAL) {
            return List.of(
                    new IndicadorDTO("Carpetas asignadas", texto(denuncias), "Carpetas fiscales"),
                    new IndicadorDTO("Diligencias pendientes", texto(actividadRepository.countByPerfilAndFechaHoraAfter(Rol.FISCAL, LocalDateTime.now())), "Por atender"),
                    new IndicadorDTO("En investigación", texto(denunciaRepository.countByEstadoFiscal("En investigación")), "Investigaciones activas"),
                    new IndicadorDTO("Con disposición", texto(denunciaRepository.countByEstadoFiscal("Con disposición")), "Disposiciones registradas"));
        }
        // Funcionario policial
        return List.of(
                new IndicadorDTO("Denuncias registradas", texto(denuncias), "Registros en la base de datos"),
                new IndicadorDTO("Actuaciones pendientes", texto(actuacionRepository.countByPerfilAndEstadoNot(Rol.POLICIA, "Completada")), "Por completar"),
                new IndicadorDTO("Informes remitidos", texto(remisionRepository.count()), "Remisiones a Fiscalía"),
                new IndicadorDTO("Requerimientos fiscales", texto(remisionRepository.countByEstado("Requerimiento")), "Pendientes de atención"));
    }

    /** Convierte un número en texto para mostrarlo en la tarjeta. */
    private String texto(long valor) {
        return String.valueOf(valor);
    }
}
