package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Documento o evidencia asociado a una denuncia (tabla "documento").
 */
@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 12)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // Denuncia a la que pertenece el documento
    @Column(name = "codigo_denuncia", nullable = false, length = 12)
    private String codigoDenuncia;

    // Código del funcionario que registró el documento
    @Column(name = "codigo_responsable", nullable = false, length = 20)
    private String codigoResponsable;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    // Validado, Pendiente o Remitido
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    // Breve descripción del contenido del documento
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    // ---- Archivo adjunto (columnas agregadas por el script 04 / clase MigracionBD) ----
    // Nombre con el que se guardó en la carpeta "uploads" (null = documento sin archivo)
    @Column(name = "archivo", length = 100)
    private String archivo;

    // Nombre original del archivo (el que se muestra y se usa al descargar)
    @Column(name = "archivo_nombre", length = 200)
    private String archivoNombre;

    // Tipo de archivo (application/pdf, image/png, video/mp4...)
    @Column(name = "archivo_tipo", length = 100)
    private String archivoTipo;

    // Tamaño en bytes
    @Column(name = "archivo_tamano")
    private Long archivoTamano;

    /** Constructor vacío obligatorio para JPA. */
    public Documento() {
    }

    public Documento(String codigo, String nombre, String codigoDenuncia, String codigoResponsable, LocalDate fecha, String estado) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.codigoDenuncia = codigoDenuncia;
        this.codigoResponsable = codigoResponsable;
        this.fecha = fecha;
        this.estado = estado;
    }

    // ----- Getters -----
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getCodigoDenuncia() { return codigoDenuncia; }
    public String getCodigoResponsable() { return codigoResponsable; }
    public LocalDate getFecha() { return fecha; }
    public String getEstado() { return estado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getArchivo() { return archivo; }
    public String getArchivoNombre() { return archivoNombre; }
    public String getArchivoTipo() { return archivoTipo; }
    public Long getArchivoTamano() { return archivoTamano; }

    /** Asocia un archivo guardado en disco al documento. */
    public void asignarArchivo(String archivo, String nombre, String tipo, long tamano) {
        this.archivo = archivo;
        this.archivoNombre = nombre;
        this.archivoTipo = tipo;
        this.archivoTamano = tamano;
    }
}
