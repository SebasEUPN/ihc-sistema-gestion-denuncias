package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.ActividadProgramada;
import com.proyecto.denuncias.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Acceso a la tabla "actividad_programada". */
public interface ActividadProgramadaRepository extends JpaRepository<ActividadProgramada, Long> {

    // Agenda policial (actividades con un policía responsable) ordenada por fecha
    List<ActividadProgramada> findByIdPoliciaIsNotNullOrderByFechaHoraAsc();

    // Agenda fiscal (diligencias con un fiscal responsable) ordenada por fecha
    List<ActividadProgramada> findByIdFiscalIsNotNullOrderByFechaHoraAsc();

    // Cantidad de diligencias fiscales posteriores a una fecha (pendientes)
    long countByIdFiscalIsNotNullAndFechaHoraAfter(LocalDateTime fecha);
}
