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
 * Copia de seguridad de la base de datos (tabla "copia_seguridad").
 * Solo un administrador puede generarla: id_admin apunta a la tabla "admin".
 * El contenido de la copia (archivo .json.gz) se guarda en "archivo_almacenado".
 */
@Entity
@Table(name = "copia_seguridad")
public class CopiaSeguridad {

    public static final String COMPLETADA = "Completada";
    public static final String FALLIDA = "Fallida";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_copia")
    private Long id;

    // Administrador que generó la copia (o que programó las copias automáticas)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_admin", nullable = false)
    private Usuario admin;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    // COMPLETA, DENUNCIAS o USUARIOS
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    // "Manual" o "Automática"
    @Column(name = "origen", nullable = false, length = 15)
    private String origen;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    // Cantidad de filas respaldadas
    @Column(name = "registros", nullable = false)
    private int registros;

    // "Completada" o "Fallida"
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    // Nombre del archivo en "archivo_almacenado" (null si la copia falló)
    @Column(name = "archivo", length = 100)
    private String archivo;

    public CopiaSeguridad() {
    }

    public CopiaSeguridad(Usuario admin, String nombre, String tipo, String descripcion, String origen) {
        this.admin = admin;
        this.nombre = nombre;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.origen = origen;
        this.fechaHora = LocalDateTime.now();
        this.estado = FALLIDA;
    }

    /** Marca la copia como completada y anota dónde quedó guardada. */
    public void completar(String archivo, long tamanoBytes, int registros) {
        this.archivo = archivo;
        this.tamanoBytes = tamanoBytes;
        this.registros = registros;
        this.estado = COMPLETADA;
    }

    public Long getId() { return id; }
    public Usuario getAdmin() { return admin; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public String getOrigen() { return origen; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public long getTamanoBytes() { return tamanoBytes; }
    public int getRegistros() { return registros; }
    public String getEstado() { return estado; }
    public String getArchivo() { return archivo; }
}
