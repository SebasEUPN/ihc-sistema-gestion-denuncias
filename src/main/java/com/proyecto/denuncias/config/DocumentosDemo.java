package com.proyecto.denuncias.config;

import com.proyecto.denuncias.service.DocumentoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, crea un PDF de demostración para los documentos que todavía
 * no tienen archivo (los documentos de ejemplo), para que se puedan ver y
 * descargar desde "Documentos y evidencias". Los que ya tienen archivo no se tocan.
 */
@Component
@Order(2) // después de MigracionBD (columnas) y de DatosIniciales (datos de ejemplo)
public class DocumentosDemo implements CommandLineRunner {

    private final DocumentoService documentoService;

    public DocumentosDemo(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @Override
    public void run(String... args) {
        documentoService.generarArchivosDemostracion();
    }
}
