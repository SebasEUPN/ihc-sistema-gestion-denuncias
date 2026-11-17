package com.proyecto.denuncias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal del proyecto.
 * Al ejecutarla, Spring Boot levanta el servidor web embebido (Tomcat),
 * se conecta a la base de datos PostgreSQL y publica la aplicación en http://localhost:8080
 */
@EnableScheduling      // Permite tareas programadas (copias de seguridad automáticas)
@SpringBootApplication // Activa la configuración automática y la búsqueda de componentes en este paquete
public class SistemaDenunciasApplication {

    public static void main(String[] args) {
        // Arranca toda la aplicación Spring Boot
        SpringApplication.run(SistemaDenunciasApplication.class, args);
    }
}
