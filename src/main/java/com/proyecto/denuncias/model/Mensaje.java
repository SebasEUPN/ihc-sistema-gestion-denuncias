package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Mensaje de una conversación (tabla "mensaje").
 * Puede ser solo texto o incluir un archivo adjunto (foto, video, audio o documento).
 */
@Entity
@Table(name = "mensaje")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conversacion", nullable = false)
    private Conversacion conversacion;

    // Funcionario que envió el mensaje; null significa que lo envió el ciudadano
    @ManyToOne
    @JoinColumn(name = "id_usuario_remitente")
    private Usuario remitente;

    @Column(name = "texto", length = 2000)
    private String texto;

    // Datos del archivo adjunto (todos null cuando el mensaje es solo texto)
    @Column(name = "adjunto_nombre", length = 200)
    private String adjuntoNombre;

    @Column(name = "adjunto_tipo", length = 100)
    private String adjuntoTipo;

    @Column(name = "adjunto_tamano")
    private Long adjuntoTamano;

    // Nombre con el que el archivo quedó guardado en la carpeta de archivos
    @Column(name = "adjunto_archivo", length = 100)
    private String adjuntoArchivo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // true cuando el destinatario ya abrió la conversación
    @Column(name = "leido", nullable = false)
    private boolean leido;

    /** Constructor vacío obligatorio para JPA. */
    public Mensaje() {
    }

    public Mensaje(Conversacion conversacion, Usuario remitente, String texto, LocalDateTime fechaHora, boolean leido) {
        this.conversacion = conversacion;
        this.remitente = remitente;
        this.texto = texto;
        this.fechaHora = fechaHora;
        this.leido = leido;
    }

    /** Indica si el mensaje fue enviado por el usuario con ese id. */
    public boolean esDe(Long idUsuario) {
        return remitente != null && remitente.getId().equals(idUsuario);
    }

    // ----- Getters y setters -----
    public Long getId() { return id; }
    public Conversacion getConversacion() { return conversacion; }
    public Usuario getRemitente() { return remitente; }
    public String getTexto() { return texto; }
    public String getAdjuntoNombre() { return adjuntoNombre; }
    public void setAdjuntoNombre(String adjuntoNombre) { this.adjuntoNombre = adjuntoNombre; }
    public String getAdjuntoTipo() { return adjuntoTipo; }
    public void setAdjuntoTipo(String adjuntoTipo) { this.adjuntoTipo = adjuntoTipo; }
    public Long getAdjuntoTamano() { return adjuntoTamano; }
    public void setAdjuntoTamano(Long adjuntoTamano) { this.adjuntoTamano = adjuntoTamano; }
    public String getAdjuntoArchivo() { return adjuntoArchivo; }
    public void setAdjuntoArchivo(String adjuntoArchivo) { this.adjuntoArchivo = adjuntoArchivo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}
