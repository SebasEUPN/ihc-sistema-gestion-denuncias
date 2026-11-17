package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    // Responsable de la actividad: un policía (tabla "policia") o un fiscal (tabla "fiscal").
    // Siempre se llena exactamente una de las dos columnas.
    @Column(name = "id_policia")
    private Long idPolicia;

    @Column(name = "id_fiscal")
    private Long idFiscal;

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

    public ActividadProgramada(Usuario responsable, String nombre, String referencia, String lugar, LocalDateTime fechaHora) {
        if (responsable.getRol() == Rol.FISCAL) {
            this.idFiscal = responsable.getId();
        } else {
            this.idPolicia = responsable.getId();
        }
        this.nombre = nombre;
        this.referencia = referencia;
        this.lugar = lugar;
        this.fechaHora = fechaHora;
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public Long getIdPolicia() { return idPolicia; }
    public Long getIdFiscal() { return idFiscal; }
    /** Perfil al que pertenece la actividad; se deduce del responsable. */
    public Rol getPerfil() { return idFiscal != null ? Rol.FISCAL : Rol.POLICIA; }
    public String getNombre() { return nombre; }
    public String getReferencia() { return referencia; }
    public String getLugar() { return lugar; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
