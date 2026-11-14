package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.RegistroActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a la tabla "registro_actividad". */
public interface RegistroActividadRepository extends JpaRepository<RegistroActividad, Long> {

    // Los 100 registros de auditoría más recientes
    List<RegistroActividad> findTop100ByOrderByFechaHoraDescIdDesc();

    // Cantidad de registros de un módulo (ej.: "Reportes")
    long countByModulo(String modulo);

    // Operaciones registradas en la auditoría por cada usuario: [código, cantidad]
    @Query("select r.codigoUsuario, count(r) from RegistroActividad r group by r.codigoUsuario")
    List<Object[]> contarPorUsuario();
}
