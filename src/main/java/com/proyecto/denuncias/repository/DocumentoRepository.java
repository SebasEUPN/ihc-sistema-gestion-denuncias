package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a la tabla "documento". */
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findAllByOrderByCodigoAsc();

    // Busca un documento por su código (DOC-001)
    java.util.Optional<Documento> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    // Documentos de una denuncia
    List<Documento> findByCodigoDenunciaOrderByFechaAscIdAsc(String codigoDenuncia);

    // Documentos que todavía no tienen archivo (para generar los de demostración)
    List<Documento> findByArchivoIsNull();

    // Documentos por estado: [estado, cantidad]
    @Query("select d.estado, count(d) from Documento d group by d.estado")
    List<Object[]> contarPorEstado();
}
