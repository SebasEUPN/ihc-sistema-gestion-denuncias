package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla "conversacion". */
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    // Conversaciones donde el usuario participa como A o como B
    List<Conversacion> findByUsuarioAIdOrUsuarioBIdOrderByIdAsc(Long idUsuarioA, Long idUsuarioB);
}
