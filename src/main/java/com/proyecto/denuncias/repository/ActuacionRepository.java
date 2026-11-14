package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.Actuacion;
import com.proyecto.denuncias.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a la tabla "actuacion". */
public interface ActuacionRepository extends JpaRepository<Actuacion, Long> {

    // Actuaciones de un perfil (policiales o fiscales)
    List<Actuacion> findByPerfilOrderByCodigoAsc(Rol perfil);

    // Cantidad de actuaciones de un perfil que todavía no tienen el estado indicado
    long countByPerfilAndEstadoNot(Rol perfil, String estado);

    // Actuaciones policiales y fiscales de una denuncia, en orden cronológico
    List<Actuacion> findByCodigoDenunciaOrderByFechaAscIdAsc(String codigoDenuncia);

    // Actuaciones por perfil y estado: [perfil, estado, cantidad]
    @Query("select a.perfil, a.estado, count(a) from Actuacion a group by a.perfil, a.estado")
    List<Object[]> contarPorPerfilYEstado();
}
