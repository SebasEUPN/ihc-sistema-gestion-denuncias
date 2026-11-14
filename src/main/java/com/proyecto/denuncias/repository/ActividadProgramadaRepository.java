package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.ActividadProgramada;
import com.proyecto.denuncias.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Acceso a la tabla "actividad_programada". */
public interface ActividadProgramadaRepository extends JpaRepository<ActividadProgramada, Long> {

    // Agenda de un perfil ordenada por fecha
    List<ActividadProgramada> findByPerfilOrderByFechaHoraAsc(Rol perfil);

    // Cantidad de actividades de un perfil posteriores a una fecha (pendientes)
    long countByPerfilAndFechaHoraAfter(Rol perfil, LocalDateTime fecha);
}
