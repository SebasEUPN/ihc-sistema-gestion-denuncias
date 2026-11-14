package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla "usuario".
 * Spring Data crea la consulta SQL a partir del nombre de cada método,
 * por eso no hace falta escribir ninguna implementación.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // SELECT * FROM usuario WHERE UPPER(codigo) = UPPER(?)
    Optional<Usuario> findByCodigoIgnoreCase(String codigo);

    // ¿Existe un usuario con ese código?
    boolean existsByCodigoIgnoreCase(String codigo);

    // ¿Existe un usuario con ese DNI?
    boolean existsByDni(String dni);

    // Todos los usuarios en el orden en que fueron creados
    List<Usuario> findAllByOrderByIdAsc();

    // Cantidad de usuarios cuyo rol es distinto al indicado (ej.: funcionarios sin contar administradores)
    long countByRolNot(Rol rol);

    // Igual que el anterior, pero solo cuentas habilitadas
    long countByRolNotAndHabilitadoTrue(Rol rol);

    // Cuentas habilitadas de un perfil (para no dejar el sistema sin administradores)
    long countByRolAndHabilitadoTrue(Rol rol);
}
