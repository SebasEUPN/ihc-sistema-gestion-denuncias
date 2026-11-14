package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.SeguimientoDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a la tabla "seguimiento_denuncia". */
public interface SeguimientoDenunciaRepository extends JpaRepository<SeguimientoDenuncia, Long> {

    // Historial de una denuncia, del registro más reciente al más antiguo
    List<SeguimientoDenuncia> findByDenunciaIdOrderByFechaHoraDesc(Long idDenuncia);

    // Actualizaciones de denuncias registradas por cada funcionario: [código, cantidad]
    @Query("select s.codigoUsuario, count(s) from SeguimientoDenuncia s group by s.codigoUsuario")
    List<Object[]> contarPorUsuario();
}
