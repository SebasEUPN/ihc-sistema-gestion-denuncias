package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Remisión de una denuncia desde la PNP hacia la Fiscalía (tabla "remision_fiscal").
 * Alimenta el módulo "Seguimiento de Fiscalía" del funcionario policial.
 */
@Entity
@Table(name = "remision_fiscal")
public class RemisionFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_remision")
    private Long id;

    @Column(name = "codigo_denuncia", nullable = false, length = 12)
    private String codigoDenuncia;

    // Carpeta fiscal asignada; null significa "por asignar"
    @Column(name = "carpeta_fiscal", length = 20)
    private String carpetaFiscal;

    @Column(name = "fecha_remision", nullable = false)
    private LocalDate fechaRemision;

    // Fecha de la última respuesta de la Fiscalía; null si aún no responde
    @Column(name = "ultima_respuesta")
    private LocalDate ultimaRespuesta;

    // Recibido, Requerimiento, En espera o Pendiente
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    /** Constructor vacío obligatorio para JPA. */
    public RemisionFiscal() {
    }

    public RemisionFiscal(String codigoDenuncia, String carpetaFiscal, LocalDate fechaRemision, LocalDate ultimaRespuesta, String estado) {
        this.codigoDenuncia = codigoDenuncia;
        this.carpetaFiscal = carpetaFiscal;
        this.fechaRemision = fechaRemision;
        this.ultimaRespuesta = ultimaRespuesta;
        this.estado = estado;
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public String getCodigoDenuncia() { return codigoDenuncia; }
    public String getCarpetaFiscal() { return carpetaFiscal; }
    public LocalDate getFechaRemision() { return fechaRemision; }
    public LocalDate getUltimaRespuesta() { return ultimaRespuesta; }
    public String getEstado() { return estado; }
}
