package com.proyecto.denuncias.service;

import com.proyecto.denuncias.model.RegistroActividad;
import com.proyecto.denuncias.repository.RegistroActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de auditoría: deja constancia de las operaciones importantes
 * (inicios de sesión, cambios de estado, registros de usuarios, etc.).
 */
@Service
public class AuditoriaService {

    private final RegistroActividadRepository registroRepository;

    // Spring entrega (inyecta) el repositorio por el constructor
    public AuditoriaService(RegistroActividadRepository registroRepository) {
        this.registroRepository = registroRepository;
    }

    /** Guarda un registro de actividad con la fecha y hora actuales. */
    @Transactional
    public void registrar(String codigoUsuario, String actividad, String modulo) {
        registroRepository.save(new RegistroActividad(LocalDateTime.now(), codigoUsuario, actividad, modulo, "Correcto"));
    }

    /** Últimos registros de actividad para la pantalla del administrador. */
    @Transactional(readOnly = true)
    public List<RegistroActividad> listar() {
        return registroRepository.findTop100ByOrderByFechaHoraDescIdDesc();
    }
}
