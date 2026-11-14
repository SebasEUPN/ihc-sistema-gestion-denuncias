package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Actuación realizada sobre una denuncia (tabla "actuacion").
 * Las policiales usan códigos ACT-001 y las fiscales FIS-001.
 */
@Entity
@Table(name = "actuacion")
public class Actuacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actuacion")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 12)
    private String codigo;

    // Código de la denuncia relacionada (clave foránea hacia denuncia.codigo)
    @Column(name = "codigo_denuncia", nullable = false, length = 12)
    private String codigoDenuncia;

    // Indica si la actuación es policial o fiscal
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 10)
    private Rol perfil;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    // Completada, En proceso, Programada o Pendiente
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    /** Constructor vacío obligatorio para JPA. */
    public Actuacion() {
    }

    public Actuacion(String codigo, String codigoDenuncia, Rol perfil, String descripcion, LocalDate fecha, String estado) {
        this.codigo = codigo;
        this.codigoDenuncia = codigoDenuncia;
        this.perfil = perfil;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.estado = estado;
    }

    // ----- Getters (se usan para convertir el objeto a JSON) -----
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getCodigoDenuncia() { return codigoDenuncia; }
    public Rol getPerfil() { return perfil; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getFecha() { return fecha; }
    public String getEstado() { return estado; }
}
