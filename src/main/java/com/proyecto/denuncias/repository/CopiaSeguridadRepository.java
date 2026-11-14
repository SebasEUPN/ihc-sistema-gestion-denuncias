package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.CopiaSeguridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla "copia_seguridad". */
public interface CopiaSeguridadRepository extends JpaRepository<CopiaSeguridad, Long> {

    // Copias de la más reciente a la más antigua
    List<CopiaSeguridad> findAllByOrderByFechaHoraDescIdDesc();
}
