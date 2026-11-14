package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ciudadano que presenta una denuncia (tabla "ciudadano").
 * El segundo prototipo (seguimiento por código) usará esta misma tabla.
 */
@Entity
@Table(name = "ciudadano")
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciudadano")
    private Long id;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "dni", nullable = false, unique = true, length = 8)
    private String dni;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "telefono", length = 15)
    private String telefono;

    /** Constructor vacío obligatorio para JPA. */
    public Ciudadano() {
    }

    public Ciudadano(String nombres, String apellidos, String dni, String correo, String telefono) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.correo = correo;
        this.telefono = telefono;
    }

    /** Nombre completo: "Juan Carlos Pérez Ramírez". */
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    /** Nombre corto para las tablas: primer nombre y primer apellido ("Juan Pérez"). */
    public String getNombreCorto() {
        return nombres.split(" ")[0] + " " + apellidos.split(" ")[0];
    }

    // ----- Getters y setters -----
    public Long getId() { return id; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
