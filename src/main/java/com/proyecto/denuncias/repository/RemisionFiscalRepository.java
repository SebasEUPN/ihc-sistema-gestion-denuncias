package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.RemisionFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a la tabla "remision_fiscal". */
public interface RemisionFiscalRepository extends JpaRepository<RemisionFiscal, Long> {

    List<RemisionFiscal> findAllByOrderByFechaRemisionAscIdAsc();

    // Cantidad de remisiones con un estado (ej.: "Requerimiento")
    long countByEstado(String estado);

    // Última remisión de una denuncia a la Fiscalía
    java.util.Optional<RemisionFiscal> findFirstByCodigoDenunciaOrderByFechaRemisionDescIdDesc(String codigoDenuncia);

    // Remisiones a Fiscalía por estado: [estado, cantidad]
    @Query("select r.estado, count(r) from RemisionFiscal r group by r.estado")
    List<Object[]> contarPorEstado();
}
