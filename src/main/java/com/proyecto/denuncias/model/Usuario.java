package com.proyecto.denuncias.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa a un funcionario (policía, fiscal o administrador).
 * Cada objeto de esta clase equivale a una fila de la tabla "usuario".
 */
@Entity
@Table(name = "usuario")
// Los datos propios de cada perfil están en tablas separadas (1 a 1 con "usuario").
// Hibernate las une con LEFT JOIN y solo guarda la fila de la tabla del perfil
// que tiene datos: un policía tiene fila en "policia", un fiscal en "fiscal" y
// un administrador en "admin".
@SecondaryTable(name = "policia", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_usuario"))
@SecondaryTable(name = "fiscal", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_usuario"))
@SecondaryTable(name = "admin", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_usuario"))
public class Usuario {

    // Clave primaria generada por SQL Server (IDENTITY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    // Código institucional con el que se inicia sesión (PNP-2045, FN-1023, ADM-0001)
    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    // Contraseña cifrada con BCrypt; nunca se guarda ni se envía la contraseña real
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "dni", nullable = false, unique = true, length = 8)
    private String dni;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "direccion", length = 150)
    private String direccion;

    @Column(name = "contacto_emergencia", length = 15)
    private String contactoEmergencia;

    // PNP, Fiscalía o Administración
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 10)
    private Rol rol;

    // ----- Tabla "policia" -----
    @Column(name = "rango", table = "policia", length = 60)
    private String policiaRango;

    @Column(name = "dependencia", table = "policia", length = 120)
    private String policiaDependencia;

    // ----- Tabla "fiscal" -----
    @Column(name = "cargo", table = "fiscal", length = 60)
    private String fiscalCargo;

    @Column(name = "despacho", table = "fiscal", length = 120)
    private String fiscalDespacho;

    // ----- Tabla "admin" -----
    @Column(name = "cargo", table = "admin", length = 60)
    private String adminCargo;

    @Column(name = "oficina", table = "admin", length = 120)
    private String adminOficina;

    @Column(name = "habilitado", nullable = false)
    private boolean habilitado = true;

    // Nombre del archivo de la foto de perfil dentro de la carpeta de archivos
    @Column(name = "foto", length = 100)
    private String foto;

    @Column(name = "foto_tipo", length = 40)
    private String fotoTipo;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Módulos habilitados para el funcionario; se guardan en la tabla "usuario_permiso"
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permiso", joinColumns = @JoinColumn(name = "id_usuario"))
    @Column(name = "modulo", length = 30)
    private Set<String> permisos = new HashSet<>();

    /** Constructor vacío obligatorio para JPA. */
    public Usuario() {
    }

    /** Une nombres y apellidos para mostrarlos en pantalla. */
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    // ----- Getters y setters -----
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getContactoEmergencia() { return contactoEmergencia; }
    public void setContactoEmergencia(String contactoEmergencia) { this.contactoEmergencia = contactoEmergencia; }
    public Rol getRol() { return rol; }

    /** Al cambiar el perfil, el rango y la dependencia pasan a la tabla del nuevo perfil. */
    public void setRol(Rol rol) {
        String rango = getRango();
        String dependencia = getDependencia();
        this.rol = rol;
        policiaRango = policiaDependencia = fiscalCargo = fiscalDespacho = adminCargo = adminOficina = null;
        setRango(rango);
        setDependencia(dependencia);
    }

    /** Institución a la que pertenece la cuenta; se deduce del perfil. */
    public String getInstitucion() {
        if (rol == Rol.POLICIA) return "PNP";
        if (rol == Rol.FISCAL) return "Fiscalía";
        return "Administración";
    }

    /** Rango (policía) o cargo (fiscal y administrador), según el perfil. */
    public String getRango() {
        if (rol == Rol.POLICIA) return policiaRango;
        if (rol == Rol.FISCAL) return fiscalCargo;
        return adminCargo;
    }

    public void setRango(String rango) {
        if (rol == Rol.POLICIA) policiaRango = rango;
        else if (rol == Rol.FISCAL) fiscalCargo = rango;
        else adminCargo = rango;
    }

    /** Dependencia policial, despacho fiscal u oficina del administrador, según el perfil. */
    public String getDependencia() {
        if (rol == Rol.POLICIA) return policiaDependencia;
        if (rol == Rol.FISCAL) return fiscalDespacho;
        return adminOficina;
    }

    public void setDependencia(String dependencia) {
        if (rol == Rol.POLICIA) policiaDependencia = dependencia;
        else if (rol == Rol.FISCAL) fiscalDespacho = dependencia;
        else adminOficina = dependencia;
    }

    /** Toda cuenta debe tener su fila en la tabla de su perfil (otras tablas la referencian). */
    @PrePersist
    @PreUpdate
    void asegurarFilaDelPerfil() {
        if (getDependencia() == null || getDependencia().isBlank()) {
            setDependencia("No registrada");
        }
    }

    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public String getFotoTipo() { return fotoTipo; }
    public void setFotoTipo(String fotoTipo) { this.fotoTipo = fotoTipo; }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public Set<String> getPermisos() { return permisos; }
    public void setPermisos(Set<String> permisos) { this.permisos = permisos; }
}
