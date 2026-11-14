package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Conversación del chat (tabla "conversacion"). Siempre tiene dos participantes:
 *  - CIUDADANO:          usuarioA (funcionario) y ciudadano
 *  - INTERINSTITUCIONAL: usuarioA (policía) y usuarioB (fiscal)
 *  - SOPORTE:            usuarioA (funcionario) y usuarioB (administrador)
 */
@Entity
@Table(name = "conversacion")
public class Conversacion {

    /** Tipos de conversación permitidos. */
    public static final String TIPO_CIUDADANO = "CIUDADANO";
    public static final String TIPO_INTERINSTITUCIONAL = "INTERINSTITUCIONAL";
    public static final String TIPO_SOPORTE = "SOPORTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Long id;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    // Denuncia relacionada (D-000584) o asunto del soporte
    @Column(name = "referencia", nullable = false, length = 40)
    private String referencia;

    // Primer participante: siempre es un funcionario
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_a", nullable = false)
    private Usuario usuarioA;

    // Segundo participante cuando la conversación es entre funcionarios
    @ManyToOne
    @JoinColumn(name = "id_usuario_b")
    private Usuario usuarioB;

    // Segundo participante cuando la conversación es con un ciudadano
    @ManyToOne
    @JoinColumn(name = "id_ciudadano")
    private Ciudadano ciudadano;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Constructor vacío obligatorio para JPA. */
    public Conversacion() {
    }

    public Conversacion(String tipo, String referencia, Usuario usuarioA, Usuario usuarioB, Ciudadano ciudadano) {
        this.tipo = tipo;
        this.referencia = referencia;
        this.usuarioA = usuarioA;
        this.usuarioB = usuarioB;
        this.ciudadano = ciudadano;
    }

    /** Indica si el usuario con ese id participa en la conversación. */
    public boolean participa(Long idUsuario) {
        return usuarioA.getId().equals(idUsuario)
                || (usuarioB != null && usuarioB.getId().equals(idUsuario));
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getReferencia() { return referencia; }
    public Usuario getUsuarioA() { return usuarioA; }
    public Usuario getUsuarioB() { return usuarioB; }
    public Ciudadano getCiudadano() { return ciudadano; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
