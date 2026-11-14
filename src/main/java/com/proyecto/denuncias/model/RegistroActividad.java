package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de auditoría (tabla "registro_actividad").
 * Guarda quién hizo qué y cuándo; lo consulta el administrador.
 */
@Entity
@Table(name = "registro_actividad")
public class RegistroActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "codigo_usuario", nullable = false, length = 20)
    private String codigoUsuario;

    @Column(name = "actividad", nullable = false, length = 250)
    private String actividad;

    // Módulo del sistema donde ocurrió (Acceso, Denuncias, Reportes...)
    @Column(name = "modulo", nullable = false, length = 40)
    private String modulo;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    /** Constructor vacío obligatorio para JPA. */
    public RegistroActividad() {
    }

    public RegistroActividad(LocalDateTime fechaHora, String codigoUsuario, String actividad, String modulo, String resultado) {
        this.fechaHora = fechaHora;
        this.codigoUsuario = codigoUsuario;
        this.actividad = actividad;
        this.modulo = modulo;
        this.resultado = resultado;
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getCodigoUsuario() { return codigoUsuario; }
    public String getActividad() { return actividad; }
    public String getModulo() { return modulo; }
    public String getResultado() { return resultado; }
}
