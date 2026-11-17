package com.proyecto.denuncias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de auditoría (tabla "registro_actividad").
 * Guarda quién hizo qué y cuándo; lo consulta el administrador.
 */
@Entity
@Table(name = "registro_actividad")
public class RegistroActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "codigo_usuario", nullable = false, length = 20)
    private String codigoUsuario;

    @Column(name = "actividad", nullable = false, length = 250)
    private String actividad;

    // Módulo del sistema donde ocurrió (Acceso, Denuncias, Reportes...)
    @Column(name = "modulo", nullable = false, length = 40)
    private String modulo;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    /** Constructor vacío obligatorio para JPA. */
    // Quién hizo la operación: se llena la columna de la tabla de su perfil
    // ("policia", "fiscal" o "admin"). Quedan vacías si el código no existe.
    @Column(name = "id_policia")
    private Long idPolicia;

    @Column(name = "id_fiscal")
    private Long idFiscal;

    @Column(name = "id_admin")
    private Long idAdmin;

    public RegistroActividad() {
    }

    public RegistroActividad(LocalDateTime fechaHora, String codigoUsuario, String actividad, String modulo, String resultado) {
        this.fechaHora = fechaHora;
        this.codigoUsuario = codigoUsuario;
        this.actividad = actividad;
        this.modulo = modulo;
        this.resultado = resultado;
    }

    // ----- Getters -----
    /** Enlaza el registro con el usuario que hizo la operación, según su perfil. */
    public RegistroActividad de(Usuario usuario) {
        if (usuario == null) {
            return this;
        }
        if (usuario.getRol() == Rol.POLICIA) {
            this.idPolicia = usuario.getId();
        } else if (usuario.getRol() == Rol.FISCAL) {
            this.idFiscal = usuario.getId();
        } else {
            this.idAdmin = usuario.getId();
        }
        return this;
    }

    public Long getId() { return id; }
    public Long getIdPolicia() { return idPolicia; }
    public Long getIdFiscal() { return idFiscal; }
    public Long getIdAdmin() { return idAdmin; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getCodigoUsuario() { return codigoUsuario; }
    public String getActividad() { return actividad; }
    public String getModulo() { return modulo; }
    public String getResultado() { return resultado; }
}
