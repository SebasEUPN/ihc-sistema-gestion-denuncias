package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.CopiaSeguridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla "copia_seguridad". */
public interface CopiaSeguridadRepository extends JpaRepository<CopiaSeguridad, Long> {

    // Historial de copias, de la más reciente a la más antigua
    List<CopiaSeguridad> findAllByOrderByFechaHoraDescIdDesc();

    // Última copia de un origen ("Automática") con cierto estado
    Optional<CopiaSeguridad> findFirstByOrigenAndEstadoOrderByFechaHoraDesc(String origen, String estado);

    // Copias de un origen anteriores a una fecha (para aplicar el tiempo de conservación)
    List<CopiaSeguridad> findByOrigenAndFechaHoraBefore(String origen, LocalDateTime fecha);

    // ¿Ya existe una copia con ese nombre?
    boolean existsByNombreIgnoreCase(String nombre);
}
