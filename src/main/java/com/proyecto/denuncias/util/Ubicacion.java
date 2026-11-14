package com.proyecto.denuncias.util;

/**
 * Obtiene el distrito y la provincia a partir del texto "lugar de los hechos".
 *
 * Las denuncias importadas del Excel usan el formato
 * "Dirección, Distrito, Provincia" (ej.: "Av. Iglesias cdra. 3, Lince, Lima").
 * Las denuncias de demostración solo traen "Dirección, Distrito" o el distrito;
 * todas son de Lima, por eso en esos casos la provincia se toma como "Lima".
 */
public final class Ubicacion {

    private Ubicacion() {
    }

    /** Distrito y provincia. */
    public record Partes(String distrito, String provincia) {
    }

    public static Partes de(String lugar) {
        if (lugar == null || lugar.isBlank()) {
            return new Partes("No registrado", "No registrado");
        }
        String[] partes = lugar.split(",");
        int n = partes.length;
        if (n >= 3) {
            return new Partes(partes[n - 2].trim(), partes[n - 1].trim());
        }
        return new Partes(partes[n - 1].trim(), "Lima");
    }
}
