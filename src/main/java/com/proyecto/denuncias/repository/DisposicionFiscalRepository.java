package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.DisposicionFiscal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla "disposicion_fiscal". */
public interface DisposicionFiscalRepository extends JpaRepository<DisposicionFiscal, Long> {

    List<DisposicionFiscal> findAllByOrderByNumeroAsc();

    // Disposiciones emitidas en una carpeta fiscal, en orden cronológico
    List<DisposicionFiscal> findByCarpetaFiscalOrderByFechaAscIdAsc(String carpetaFiscal);
}
