package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Historial de copias de seguridad (tabla "copia_seguridad").
 * En el prototipo el respaldo es simulado: solo se registra la operación.
 */
@Entity
@Table(name = "copia_seguridad")
public class CopiaSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_copia")
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Tamaño del respaldo en megabytes
    @Column(name = "tamano_mb", nullable = false)
    private int tamanoMb;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    // Administrador que generó la copia
    @Column(name = "codigo_usuario", nullable = false, length = 20)
    private String codigoUsuario;

    /** Constructor vacío obligatorio para JPA. */
    public CopiaSeguridad() {
    }

    public CopiaSeguridad(LocalDateTime fechaHora, int tamanoMb, String estado, String codigoUsuario) {
        this.fechaHora = fechaHora;
        this.tamanoMb = tamanoMb;
        this.estado = estado;
        this.codigoUsuario = codigoUsuario;
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public int getTamanoMb() { return tamanoMb; }
    public String getEstado() { return estado; }
    public String getCodigoUsuario() { return codigoUsuario; }
}
