package com.proyecto.denuncias.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generador de archivos Excel (.xlsx) sencillo, escrito sin librerías externas.
 *
 * Un .xlsx es en realidad un archivo ZIP con varios XML dentro: la lista de
 * contenidos, el libro, los estilos y la hoja con las celdas. Esta clase
 * arma esos XML con una sola hoja.
 *
 * Uso:
 *   XlsxSimple excel = new XlsxSimple("Reporte");
 *   excel.fila(XlsxSimple.TITULO, "Reporte de denuncias");
 *   excel.fila(XlsxSimple.ENCABEZADO, "Código", "Delito");   // marca la fila de filtros
 *   excel.fila(XlsxSimple.CELDA, "D-000584", "Hurto");
 *   excel.anchos(14, 22);
 *   byte[] archivo = excel.generar();
 */
public class XlsxSimple {

    /** Estilos disponibles (posición en la lista "cellXfs" de styles.xml). */
    public static final int NORMAL = 0;
    public static final int TITULO = 1;      // negrita grande
    public static final int ENCABEZADO = 2;  // negrita, fondo azul claro y borde
    public static final int CELDA = 3;       // con borde
    public static final int ETIQUETA = 4;    // negrita

    private final String nombreHoja;
    private final List<String> filasXml = new ArrayList<>();
    private double[] anchos = new double[0];
    private int filaEncabezado = -1;   // fila donde se activa el filtro de Excel
    private int columnasEncabezado = 0;

    public XlsxSimple(String nombreHoja) {
        this.nombreHoja = nombreHoja;
    }

    /** Agrega una fila con el estilo indicado. Una fila ENCABEZADO activa los filtros y queda fija al desplazarse. */
    public void fila(int estilo, String... valores) {
        int numero = filasXml.size() + 1;
        if (estilo == ENCABEZADO) {
            filaEncabezado = numero;
            columnasEncabezado = valores.length;
        }
        StringBuilder xml = new StringBuilder("<row r=\"").append(numero).append("\">");
        for (int i = 0; i < valores.length; i++) {
            String valor = valores[i] == null ? "" : valores[i];
            // Celda de texto "en línea" (no necesita la tabla de textos compartidos)
            xml.append("<c r=\"").append(columna(i)).append(numero).append("\" t=\"inlineStr\" s=\"").append(estilo)
                    .append("\"><is><t xml:space=\"preserve\">").append(escapar(valor)).append("</t></is></c>");
        }
        filasXml.add(xml.append("</row>").toString());
    }

    /** Fila vacía (separación). */
    public void filaVacia() {
        filasXml.add("<row r=\"" + (filasXml.size() + 1) + "\"/>");
    }

    /** Ancho de cada columna, en caracteres aproximados. */
    public void anchos(double... anchos) {
        this.anchos = anchos;
    }

    /** Arma el archivo .xlsx completo y lo devuelve como bytes. */
    public byte[] generar() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            agregar(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                    </Types>""");
            agregar(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            agregar(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets><sheet name="%s" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>""".formatted(escapar(nombreHoja)));
            agregar(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                    </Relationships>""");
            // Estilos: fuentes, rellenos, bordes y las 5 combinaciones usadas (NORMAL, TITULO, ENCABEZADO, CELDA, ETIQUETA)
            agregar(zip, "xl/styles.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <fonts count="3"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="14"/><color rgb="FF173B63"/><name val="Calibri"/></font></fonts>
                    <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFDDEBF7"/><bgColor indexed="64"/></patternFill></fill></fills>
                    <borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color rgb="FFBFCCDA"/></left><right style="thin"><color rgb="FFBFCCDA"/></right><top style="thin"><color rgb="FFBFCCDA"/></top><bottom style="thin"><color rgb="FFBFCCDA"/></bottom><diagonal/></border></borders>
                    <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                    <cellXfs count="5">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    </cellXfs>
                    <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                    </styleSheet>""");
            agregar(zip, "xl/worksheets/sheet1.xml", hoja());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
        return salida.toByteArray();
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo
    // ------------------------------------------------------------------

    private String hoja() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        // La fila de encabezados queda fija al bajar por la hoja
        if (filaEncabezado > 0) {
            xml.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"").append(filaEncabezado)
                    .append("\" topLeftCell=\"A").append(filaEncabezado + 1)
                    .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        }
        if (anchos.length > 0) {
            xml.append("<cols>");
            for (int i = 0; i < anchos.length; i++) {
                xml.append(String.format(Locale.ROOT, "<col min=\"%d\" max=\"%d\" width=\"%.1f\" customWidth=\"1\"/>", i + 1, i + 1, anchos[i]));
            }
            xml.append("</cols>");
        }
        xml.append("<sheetData>");
        filasXml.forEach(xml::append);
        xml.append("</sheetData>");
        // Botones de filtro en la fila de encabezados
        if (filaEncabezado > 0 && columnasEncabezado > 0) {
            xml.append("<autoFilter ref=\"A").append(filaEncabezado).append(":").append(columna(columnasEncabezado - 1))
                    .append(Math.max(filaEncabezado, filasXml.size())).append("\"/>");
        }
        return xml.append("</worksheet>").toString();
    }

    private void agregar(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** 0 -> A, 1 -> B, ..., 26 -> AA */
    private String columna(int indice) {
        StringBuilder letras = new StringBuilder();
        for (int n = indice + 1; n > 0; n = (n - 1) / 26) {
            letras.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return letras.toString();
    }

    /** Escapa los caracteres especiales de XML y quita los caracteres de control no permitidos. */
    private String escapar(String texto) {
        StringBuilder resultado = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '&' -> resultado.append("&amp;");
                case '<' -> resultado.append("&lt;");
                case '>' -> resultado.append("&gt;");
                case '"' -> resultado.append("&quot;");
                default -> {
                    if (c >= 0x20 || c == '\n' || c == '\t') resultado.append(c);
                }
            }
        }
        return resultado.toString();
    }
}
