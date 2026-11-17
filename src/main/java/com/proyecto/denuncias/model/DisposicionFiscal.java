package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Disposición emitida por la Fiscalía en una carpeta fiscal (tabla "disposicion_fiscal").
 */
@Entity
@Table(name = "disposicion_fiscal")
public class DisposicionFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disposicion")
    private Long id;

    // Número de la disposición (DISP-2026-0012)
    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    @Column(name = "carpeta_fiscal", nullable = false, length = 20)
    private String carpetaFiscal;

    @Column(name = "tipo", nullable = false, length = 120)
    private String tipo;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "fiscal_responsable", nullable = false, length = 120)
    private String fiscalResponsable;

    // Emitida o Notificada
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    /** Constructor vacío obligatorio para JPA. */
    // Fiscal que emite la disposición (tabla "fiscal")
    @Column(name = "id_fiscal")
    private Long idFiscal;

    // Funcionario policial a cargo de la denuncia, a quien se comunica (tabla "policia")
    @Column(name = "id_policia")
    private Long idPolicia;

    public DisposicionFiscal() {
    }

    public DisposicionFiscal(String numero, String carpetaFiscal, String tipo, LocalDate fecha, String fiscalResponsable, String estado) {
        this.numero = numero;
        this.carpetaFiscal = carpetaFiscal;
        this.tipo = tipo;
        this.fecha = fecha;
        this.fiscalResponsable = fiscalResponsable;
        this.estado = estado;
    }

    // ----- Getters -----
    /** Enlaza la disposición con el fiscal que la emite y el policía a cargo de la denuncia. */
    public DisposicionFiscal vincular(Usuario fiscal, Usuario policia) {
        this.idFiscal = fiscal == null ? null : fiscal.getId();
        this.idPolicia = policia == null ? null : policia.getId();
        return this;
    }

    public Long getId() { return id; }
    public Long getIdFiscal() { return idFiscal; }
    public Long getIdPolicia() { return idPolicia; }
    public String getNumero() { return numero; }
    public String getCarpetaFiscal() { return carpetaFiscal; }
    public String getTipo() { return tipo; }
    public LocalDate getFecha() { return fecha; }
    public String getFiscalResponsable() { return fiscalResponsable; }
    public String getEstado() { return estado; }
}
