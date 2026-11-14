package com.proyecto.denuncias.repository;

import com.proyecto.denuncias.model.Denuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla "denuncia". */
public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {

    // Busca una denuncia por su código visible (D-000584)
    Optional<Denuncia> findByCodigo(String codigo);

    // Todas las denuncias ordenadas por código
    List<Denuncia> findAllByOrderByCodigoAsc();

    // Denuncias registradas entre dos fechas (para los reportes)
    List<Denuncia> findByFechaRegistroBetweenOrderByCodigoAsc(LocalDateTime desde, LocalDateTime hasta);

    // Cantidad de denuncias con un estado fiscal determinado
    long countByEstadoFiscal(String estadoFiscal);

    // Todas las denuncias junto con su ciudadano en UNA sola consulta ("join fetch").
    // Sin esto, Hibernate haría una consulta extra por cada ciudadano (más de 500).
    @Query("select d from Denuncia d join fetch d.ciudadano order by d.codigo")
    List<Denuncia> listarConCiudadano();

    // Denuncias cuyos códigos están en la lista
    List<Denuncia> findByCodigoIn(java.util.Collection<String> codigos);

    // Denuncia a la que pertenece una carpeta fiscal
    Optional<Denuncia> findFirstByCarpetaFiscal(String carpetaFiscal);

    // Funcionario policial responsable de una denuncia: [código, nombres, apellidos, rango].
    // Consulta SQL directa porque id_usuario_responsable la agrega el script 02 y no está en la entidad.
    @Query(value = "select u.codigo, u.nombres, u.apellidos, u.rango from denuncia d join usuario u on u.id_usuario = d.id_usuario_responsable where d.codigo = :codigo", nativeQuery = true)
    List<Object[]> responsablePolicial(@Param("codigo") String codigo);

    // ------------------------------------------------------------------
    // Consultas para el tablero de estadísticas del administrador.
    // Se escriben en JPQL (se usan los nombres de la clase Java, no los de la tabla)
    // y cada fila del resultado llega como un arreglo: [nombre, cantidad].
    // ------------------------------------------------------------------

    // Cantidad de denuncias por cada estado policial
    @Query("select d.estadoPolicial, count(d) from Denuncia d group by d.estadoPolicial")
    List<Object[]> contarPorEstadoPolicial();

    // Cantidad de denuncias por cada estado fiscal
    @Query("select d.estadoFiscal, count(d) from Denuncia d group by d.estadoFiscal")
    List<Object[]> contarPorEstadoFiscal();

    // Cantidad de denuncias por tipo de delito
    @Query("select d.tipoDelito, count(d) from Denuncia d group by d.tipoDelito")
    List<Object[]> contarPorTipoDelito();

    // Carpetas de cada fiscal responsable por estado: [fiscal, estado, cantidad]
    @Query("select d.fiscalResponsable, d.estadoFiscal, count(d) from Denuncia d where d.fiscalResponsable is not null group by d.fiscalResponsable, d.estadoFiscal")
    List<Object[]> contarPorFiscalYEstado();

    // Fechas de registro de todas las denuncias (para agruparlas por mes)
    @Query("select d.fechaRegistro from Denuncia d")
    List<LocalDateTime> fechasDeRegistro();

    // [fecha de registro, última actualización] de las denuncias resueltas (para el tiempo promedio)
    @Query("select d.fechaRegistro, d.ultimaActualizacion from Denuncia d where d.estadoPolicial = 'Resuelto' and d.ultimaActualizacion is not null")
    List<Object[]> periodosDeResueltas();

    // Denuncias de cada funcionario policial por estado: [id del usuario, estado, cantidad].
    // Es una consulta SQL directa (nativeQuery) porque la columna id_usuario_responsable
    // la agrega el script 02_portal_ciudadano.sql y no forma parte de la entidad Denuncia.
    @Query(value = "select id_usuario_responsable, estado_policial, count(*) from denuncia where id_usuario_responsable is not null group by id_usuario_responsable, estado_policial", nativeQuery = true)
    List<Object[]> contarPorResponsableYEstado();
}
