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
    @Column(name = "institucion", nullable = false, length = 30)
    private String institucion;

    // El perfil se guarda como texto (ADMIN, POLICIA, FISCAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 10)
    private Rol rol;

    @Column(name = "rango", length = 60)
    private String rango;

    @Column(name = "dependencia", length = 120)
    private String dependencia;

    // true = la cuenta puede iniciar sesión
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
    public String getInstitucion() { return institucion; }
    public void setInstitucion(String institucion) { this.institucion = institucion; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getRango() { return rango; }
    public void setRango(String rango) { this.rango = rango; }
    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }
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
