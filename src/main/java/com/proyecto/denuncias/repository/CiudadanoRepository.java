package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.Ciudadano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a la tabla "ciudadano". */
public interface CiudadanoRepository extends JpaRepository<Ciudadano, Long> {

    // Busca un ciudadano por su DNI (lo usará el segundo prototipo)
    Optional<Ciudadano> findByDni(String dni);
}
