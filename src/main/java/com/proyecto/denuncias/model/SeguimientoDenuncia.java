package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Historial de una denuncia (tabla "seguimiento_denuncia").
 * Cada vez que un funcionario cambia el estado o agrega una observación
 * se guarda una fila; así se obtiene la línea de tiempo del expediente.
 */
@Entity
@Table(name = "seguimiento_denuncia")
public class SeguimientoDenuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguimiento")
    private Long id;

    // Denuncia a la que pertenece el registro (se carga solo cuando se necesita)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_denuncia", nullable = false)
    private Denuncia denuncia;

    // Código del funcionario que realizó el cambio
    @Column(name = "codigo_usuario", nullable = false, length = 20)
    private String codigoUsuario;

    // Perfil con el que se hizo el cambio (POLICIA o FISCAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 10)
    private Rol perfil;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Constructor vacío obligatorio para JPA. */
    public SeguimientoDenuncia() {
    }

    public SeguimientoDenuncia(Denuncia denuncia, String codigoUsuario, Rol perfil,
                               String estado, String observacion, LocalDateTime fechaHora) {
        this.denuncia = denuncia;
        this.codigoUsuario = codigoUsuario;
        this.perfil = perfil;
        this.estado = estado;
        this.observacion = observacion;
        this.fechaHora = fechaHora;
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public Denuncia getDenuncia() { return denuncia; }
    public String getCodigoUsuario() { return codigoUsuario; }
    public Rol getPerfil() { return perfil; }
    public String getEstado() { return estado; }
    public String getObservacion() { return observacion; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
