package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a la tabla "mensaje". */
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Mensajes de una conversación en orden cronológico
    List<Mensaje> findByConversacionIdOrderByFechaHoraAscIdAsc(Long idConversacion);

    // Mensajes de chat enviados por cada funcionario: [id del usuario, cantidad]
    @Query("select m.remitente.id, count(m) from Mensaje m where m.remitente is not null group by m.remitente.id")
    List<Object[]> contarPorRemitente();
}
