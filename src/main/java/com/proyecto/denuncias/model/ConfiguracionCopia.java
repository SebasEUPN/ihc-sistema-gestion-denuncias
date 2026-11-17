package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Programación de las copias de seguridad automáticas (tabla "configuracion_copia").
 * Tiene una sola fila (id 1). id_admin es el administrador que guardó la configuración.
 */
@Entity
@Table(name = "configuracion_copia")
public class ConfiguracionCopia {

    public static final int ID_UNICO = 1;

    @Id
    @Column(name = "id_configuracion")
    private Integer id = ID_UNICO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_admin", nullable = false)
    private Usuario admin;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    // DIARIA, SEMANAL o MENSUAL
    @Column(name = "frecuencia", nullable = false, length = 15)
    private String frecuencia = "DIARIA";

    // Hora programada en formato HH:mm (hora de Perú)
    @Column(name = "hora", nullable = false, length = 5)
    private String hora = "02:00";

    @Column(name = "dias_conservacion", nullable = false)
    private int diasConservacion = 30;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    public ConfiguracionCopia() {
    }

    public void actualizar(Usuario admin, boolean activo, String frecuencia, String hora, int diasConservacion) {
        this.admin = admin;
        this.activo = activo;
        this.frecuencia = frecuencia;
        this.hora = hora;
        this.diasConservacion = diasConservacion;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Usuario getAdmin() { return admin; }
    public boolean isActivo() { return activo; }
    public String getFrecuencia() { return frecuencia; }
    public String getHora() { return hora; }
    public int getDiasConservacion() { return diasConservacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
