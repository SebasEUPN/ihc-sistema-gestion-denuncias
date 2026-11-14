package com.proyecto.denuncias.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generador de archivos PDF sencillo, escrito sin librerías externas.
 *
 * Un PDF es un archivo de texto con "objetos" numerados (catálogo, páginas,
 * fuentes y el contenido de cada página) más una tabla final ("xref") con la
 * posición en bytes de cada objeto. Esta clase arma esa estructura usando
 * las fuentes estándar Helvetica y Helvetica-Bold, que todo lector de PDF
 * trae incorporadas, con la codificación WinAnsi (admite tildes y la ñ).
 *
 * Uso:
 *   PdfSimple pdf = new PdfSimple(true);            // true = hoja horizontal
 *   pdf.titulo("Reporte de denuncias");
 *   pdf.parrafo("Texto...", 10);
 *   pdf.tabla(encabezados, anchos, filas);
 *   byte[] archivo = pdf.generar("Pie de página");
 */
public class PdfSimple {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    private final float ancho;          // ancho de la hoja en puntos (1 punto = 1/72 de pulgada)
    private final float alto;           // alto de la hoja en puntos
    private final float margen = 40;
    private final List<StringBuilder> paginas = new ArrayList<>();
    private StringBuilder actual;       // contenido de la página que se está escribiendo
    private float y;                    // posición vertical actual (en PDF el 0 está abajo)

    /** horizontal = true para hoja A4 apaisada (útil para tablas anchas). */
    public PdfSimple(boolean horizontal) {
        this.ancho = horizontal ? 842 : 595;
        this.alto = horizontal ? 595 : 842;
        nuevaPagina();
    }

    /** Ancho disponible para escribir (sin márgenes). */
    public float anchoUtil() {
        return ancho - 2 * margen;
    }

    // ------------------------------------------------------------------
    // Elementos que se pueden agregar al documento
    // ------------------------------------------------------------------

    /** Título grande en negrita. */
    public void titulo(String texto) {
        asegurarEspacio(30);
        escribir(margen, y - 16, 16, true, texto, 0.06f, 0.16f, 0.29f);
        y -= 26;
    }

    /** Subtítulo en negrita de tamaño medio. */
    public void subtitulo(String texto) {
        asegurarEspacio(22);
        escribir(margen, y - 11, 11, true, texto, 0.06f, 0.16f, 0.29f);
        y -= 18;
    }

    /** Par "Etiqueta: valor" en una línea. */
    public void dato(String etiqueta, String valor) {
        asegurarEspacio(15);
        escribir(margen, y - 10, 9.5f, true, etiqueta + ":", 0.2f, 0.25f, 0.3f);
        float desplazamiento = anchoTexto(etiqueta + ": ", 9.5f, true);
        escribir(margen + desplazamiento, y - 10, 9.5f, false, recortar(valor, anchoUtil() - desplazamiento, 9.5f, false), 0, 0, 0);
        y -= 14;
    }

    /** Párrafo que se divide en varias líneas cuando no entra en el ancho de la hoja. */
    public void parrafo(String texto, float tamano) {
        for (String linea : dividirEnLineas(texto == null ? "" : texto, anchoUtil(), tamano)) {
            asegurarEspacio(tamano + 5);
            escribir(margen, y - tamano, tamano, false, linea, 0.12f, 0.15f, 0.2f);
            y -= tamano + 4.5f;
        }
        y -= 4;
    }

    /** Línea horizontal separadora. */
    public void separador() {
        asegurarEspacio(10);
        actual.append(String.format(Locale.ROOT, "0.75 0.8 0.86 RG 0.8 w %.1f %.1f m %.1f %.1f l S%n", margen, y - 4, ancho - margen, y - 4));
        y -= 12;
    }

    /** Espacio vertical en blanco. */
    public void espacio(float puntos) {
        y -= puntos;
    }

    /**
     * Tabla con encabezado sombreado. Los textos que no entran en su columna
     * se recortan con "...". Si la tabla no cabe, continúa en la página
     * siguiente repitiendo el encabezado.
     */
    public void tabla(String[] encabezados, float[] anchos, List<String[]> filas) {
        float alturaFila = 16, tamano = 8;
        dibujarEncabezado(encabezados, anchos, alturaFila, tamano);
        boolean sombreada = false;
        for (String[] fila : filas) {
            if (y - alturaFila < margen + 20) {          // no hay espacio: nueva página con encabezado
                nuevaPagina();
                dibujarEncabezado(encabezados, anchos, alturaFila, tamano);
            }
            if (sombreada) {                              // filas alternadas con fondo suave
                actual.append(String.format(Locale.ROOT, "0.965 0.975 0.985 rg %.1f %.1f %.1f %.1f re f%n", margen, y - alturaFila, sumar(anchos), alturaFila));
            }
            float x = margen;
            for (int i = 0; i < anchos.length; i++) {
                String valor = i < fila.length && fila[i] != null ? fila[i] : "";
                escribir(x + 4, y - 11, tamano, false, recortar(valor, anchos[i] - 8, tamano, false), 0.1f, 0.12f, 0.16f);
                x += anchos[i];
            }
            y -= alturaFila;
            sombreada = !sombreada;
        }
        y -= 8;
    }

    /**
     * Arma el archivo PDF completo y lo devuelve como bytes.
     * piePagina: texto que se escribe al pie de cada hoja junto al número de página.
     */
    public byte[] generar(String piePagina) {
        // Pie de página con numeración "Página X de N"
        for (int i = 0; i < paginas.size(); i++) {
            StringBuilder pagina = paginas.get(i);
            String numero = "Página " + (i + 1) + " de " + paginas.size();
            pagina.append(textoPdf(margen, 22, 7.5f, false, recortar(piePagina, anchoUtil() - 90, 7.5f, false), 0.45f, 0.5f, 0.56f));
            pagina.append(textoPdf(ancho - margen - anchoTexto(numero, 7.5f, false), 22, 7.5f, false, numero, 0.45f, 0.5f, 0.56f));
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        List<Integer> posiciones = new ArrayList<>();   // posición en bytes donde empieza cada objeto
        escribirAscii(salida, "%PDF-1.4\n%âãÏÓ\n");

        int totalObjetos = 4 + paginas.size() * 2;
        // Objeto 1: catálogo (raíz del documento)
        objeto(salida, posiciones, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        // Objeto 2: lista de páginas
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < paginas.size(); i++) {
            hijos.append(5 + i * 2).append(" 0 R ");
        }
        objeto(salida, posiciones, 2, "<< /Type /Pages /Kids [" + hijos + "] /Count " + paginas.size() + " >>");
        // Objetos 3 y 4: fuentes normal y negrita
        objeto(salida, posiciones, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objeto(salida, posiciones, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        // Por cada página: el objeto página y el objeto con su contenido
        for (int i = 0; i < paginas.size(); i++) {
            int numeroPagina = 5 + i * 2, numeroContenido = numeroPagina + 1;
            objeto(salida, posiciones, numeroPagina, String.format(Locale.ROOT,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.0f %.0f] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents %d 0 R >>",
                    ancho, alto, numeroContenido));
            byte[] contenido = paginas.get(i).toString().getBytes(WIN_ANSI);
            posiciones.add(salida.size());
            escribirAscii(salida, numeroContenido + " 0 obj\n<< /Length " + contenido.length + " >>\nstream\n");
            salida.writeBytes(contenido);
            escribirAscii(salida, "\nendstream\nendobj\n");
        }

        // Tabla de referencias: dónde empieza cada objeto
        int inicioXref = salida.size();
        StringBuilder xref = new StringBuilder("xref\n0 " + (totalObjetos + 1) + "\n0000000000 65535 f \n");
        for (int posicion : posiciones) {
            xref.append(String.format("%010d 00000 n \n", posicion));
        }
        xref.append("trailer\n<< /Size ").append(totalObjetos + 1).append(" /Root 1 0 R >>\nstartxref\n").append(inicioXref).append("\n%%EOF\n");
        escribirAscii(salida, xref.toString());
        return salida.toByteArray();
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------

    private void nuevaPagina() {
        actual = new StringBuilder();
        paginas.add(actual);
        y = alto - margen;
    }

    /** Si no queda el espacio indicado en la página, se pasa a la siguiente. */
    private void asegurarEspacio(float necesario) {
        if (y - necesario < margen + 20) {
            nuevaPagina();
        }
    }

    private void dibujarEncabezado(String[] encabezados, float[] anchos, float alturaFila, float tamano) {
        actual.append(String.format(Locale.ROOT, "0.87 0.92 0.97 rg %.1f %.1f %.1f %.1f re f%n", margen, y - alturaFila, sumar(anchos), alturaFila));
        float x = margen;
        for (int i = 0; i < anchos.length; i++) {
            escribir(x + 4, y - 11, tamano, true, recortar(encabezados[i], anchos[i] - 8, tamano, true), 0.06f, 0.16f, 0.29f);
            x += anchos[i];
        }
        y -= alturaFila;
    }

    private void escribir(float x, float yTexto, float tamano, boolean negrita, String texto, float r, float g, float b) {
        actual.append(textoPdf(x, yTexto, tamano, negrita, texto, r, g, b));
    }

    /** Instrucciones PDF para escribir un texto en una posición, con tamaño y color. */
    private String textoPdf(float x, float yTexto, float tamano, boolean negrita, String texto, float r, float g, float b) {
        return String.format(Locale.ROOT, "BT %.3f %.3f %.3f rg /%s %.1f Tf %.1f %.1f Td (%s) Tj ET%n",
                r, g, b, negrita ? "F2" : "F1", tamano, x, yTexto, escapar(texto));
    }

    /** Escapa los caracteres especiales del PDF y reemplaza los que WinAnsi no admite. */
    private String escapar(String texto) {
        CharsetEncoder codificador = WIN_ANSI.newEncoder();
        StringBuilder resultado = new StringBuilder();
        for (char c : (texto == null ? "" : texto).toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') {
                resultado.append('\\').append(c);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                resultado.append(' ');
            } else if (codificador.canEncode(c)) {
                resultado.append(c);
            } else {
                resultado.append('?');
            }
        }
        return resultado.toString();
    }

    /** Recorta el texto con "..." para que no supere el ancho indicado. */
    private String recortar(String texto, float anchoMaximo, float tamano, boolean negrita) {
        String t = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
        if (anchoTexto(t, tamano, negrita) <= anchoMaximo) {
            return t;
        }
        while (!t.isEmpty() && anchoTexto(t + "...", tamano, negrita) > anchoMaximo) {
            t = t.substring(0, t.length() - 1);
        }
        return t.trim() + "...";
    }

    /** Divide un texto largo en líneas que entren en el ancho indicado. */
    private List<String> dividirEnLineas(String texto, float anchoMaximo, float tamano) {
        List<String> lineas = new ArrayList<>();
        for (String bloque : texto.split("\n")) {
            StringBuilder linea = new StringBuilder();
            for (String palabra : bloque.split(" ")) {
                String prueba = linea.isEmpty() ? palabra : linea + " " + palabra;
                if (anchoTexto(prueba, tamano, false) > anchoMaximo && !linea.isEmpty()) {
                    lineas.add(linea.toString());
                    linea = new StringBuilder(palabra);
                } else {
                    linea = new StringBuilder(prueba);
                }
            }
            lineas.add(linea.toString());
        }
        return lineas;
    }

    /**
     * Ancho aproximado de un texto en Helvetica. Cada letra ocupa una fracción
     * distinta del tamaño de la fuente (la "i" es angosta, la "m" es ancha).
     */
    private float anchoTexto(String texto, float tamano, boolean negrita) {
        float total = 0;
        for (char c : texto.toCharArray()) {
            float f;
            if ("il.,:;|!'Ííj".indexOf(c) >= 0) f = 0.24f;
            else if ("ftrI()[] -/".indexOf(c) >= 0) f = 0.32f;
            else if ("mwMW".indexOf(c) >= 0) f = 0.86f;
            else if (Character.isUpperCase(c)) f = 0.68f;
            else if (Character.isDigit(c)) f = 0.56f;
            else f = 0.54f;
            total += f;
        }
        return total * tamano * (negrita ? 1.06f : 1f);
    }

    private float sumar(float[] valores) {
        float total = 0;
        for (float v : valores) total += v;
        return total;
    }

    private void objeto(ByteArrayOutputStream salida, List<Integer> posiciones, int numero, String contenido) {
        posiciones.add(salida.size());
        escribirAscii(salida, numero + " 0 obj\n" + contenido + "\nendobj\n");
    }

    private void escribirAscii(ByteArrayOutputStream salida, String texto) {
        salida.writeBytes(texto.getBytes(StandardCharsets.ISO_8859_1));
    }
}
