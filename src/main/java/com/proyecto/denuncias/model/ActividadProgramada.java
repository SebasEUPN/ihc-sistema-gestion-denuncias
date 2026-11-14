package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Actividad del calendario (tabla "actividad_programada").
 * Perfil POLICIA = "Programación de actividades"; perfil FISCAL = "Diligencias".
 */
@Entity
@Table(name = "actividad_programada")
public class ActividadProgramada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 10)
    private Rol perfil;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // Denuncia o carpeta fiscal con la que se relaciona la actividad
    @Column(name = "referencia", nullable = false, length = 20)
    private String referencia;

    @Column(name = "lugar", length = 150)
    private String lugar;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Constructor vacío obligatorio para JPA. */
    public ActividadProgramada() {
    }

    public ActividadProgramada(Rol perfil, String nombre, String referencia, String lugar, LocalDateTime fechaHora) {
        this.perfil = perfil;
        this.nombre = nombre;
        this.referencia = referencia;
        this.lugar = lugar;
        this.fechaHora = fechaHora;
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public Rol getPerfil() { return perfil; }
    public String getNombre() { return nombre; }
    public String getReferencia() { return referencia; }
    public String getLugar() { return lugar; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
