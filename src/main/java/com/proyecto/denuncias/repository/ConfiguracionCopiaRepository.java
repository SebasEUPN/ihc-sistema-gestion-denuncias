package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.ConfiguracionCopia;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla "configuracion_copia" (una sola fila). */
public interface ConfiguracionCopiaRepository extends JpaRepository<ConfiguracionCopia, Integer> {
}
