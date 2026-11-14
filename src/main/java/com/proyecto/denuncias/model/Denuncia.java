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
 * Denuncia registrada (tabla "denuncia"). Es el expediente principal que
 * comparten la PNP, la Fiscalía y el ciudadano que hace el seguimiento.
 */
@Entity
@Table(name = "denuncia")
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_denuncia")
    private Long id;

    // Código visible de la denuncia (D-000584)
    @Column(name = "codigo", nullable = false, unique = true, length = 12)
    private String codigo;

    // Muchas denuncias pueden pertenecer a un mismo ciudadano
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_ciudadano", nullable = false)
    private Ciudadano ciudadano;

    @Column(name = "tipo_delito", nullable = false, length = 60)
    private String tipoDelito;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "lugar_hechos", length = 200)
    private String lugarHechos;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;

    // Estado que administra el funcionario policial
    @Column(name = "estado_policial", nullable = false, length = 30)
    private String estadoPolicial;

    // Estado que administra el funcionario fiscal
    @Column(name = "estado_fiscal", nullable = false, length = 30)
    private String estadoFiscal;

    @Column(name = "carpeta_fiscal", length = 20)
    private String carpetaFiscal;

    @Column(name = "despacho_fiscal", length = 120)
    private String despachoFiscal;

    @Column(name = "fiscal_responsable", length = 120)
    private String fiscalResponsable;

    /** Constructor vacío obligatorio para JPA. */
    public Denuncia() {
    }

    // ----- Getters y setters -----
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Ciudadano getCiudadano() { return ciudadano; }
    public void setCiudadano(Ciudadano ciudadano) { this.ciudadano = ciudadano; }
    public String getTipoDelito() { return tipoDelito; }
    public void setTipoDelito(String tipoDelito) { this.tipoDelito = tipoDelito; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getLugarHechos() { return lugarHechos; }
    public void setLugarHechos(String lugarHechos) { this.lugarHechos = lugarHechos; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
    public String getEstadoPolicial() { return estadoPolicial; }
    public void setEstadoPolicial(String estadoPolicial) { this.estadoPolicial = estadoPolicial; }
    public String getEstadoFiscal() { return estadoFiscal; }
    public void setEstadoFiscal(String estadoFiscal) { this.estadoFiscal = estadoFiscal; }
    public String getCarpetaFiscal() { return carpetaFiscal; }
    public void setCarpetaFiscal(String carpetaFiscal) { this.carpetaFiscal = carpetaFiscal; }
    public String getDespachoFiscal() { return despachoFiscal; }
    public void setDespachoFiscal(String despachoFiscal) { this.despachoFiscal = despachoFiscal; }
    public String getFiscalResponsable() { return fiscalResponsable; }
    public void setFiscalResponsable(String fiscalResponsable) { this.fiscalResponsable = fiscalResponsable; }
}
