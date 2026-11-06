package com.vetcare.facturacion.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vetcare.facturacion.model.Factura;
import com.vetcare.facturacion.model.ItemFactura;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * Genera el PDF de una factura (HU-067 y HU-070).
 */
@Service
public class DocumentoFacturaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public byte[] pdf(Factura factura) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document();
        PdfWriter.getInstance(documento, salida);
        documento.open();
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        documento.add(new Paragraph("VetCare - Factura " + factura.getNumero(), titulo));
        documento.add(new Paragraph("Fecha: " + FECHA.format(factura.getFecha())));
        documento.add(new Paragraph("Mascota: " + factura.getMascotaNombre()));
        documento.add(new Paragraph("Estado: " + factura.getEstado()));
        documento.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(new float[] {6, 1, 2, 2});
        tabla.setWidthPercentage(100);
        for (String cabecera : new String[] {"Concepto", "Cant.", "Valor unitario", "Subtotal"}) {
            tabla.addCell(cabecera);
        }
        for (ItemFactura item : factura.getItems()) {
            tabla.addCell(item.descripcion());
            tabla.addCell(String.valueOf(item.cantidad()));
            tabla.addCell(pesos(item.precioUnitario()));
            tabla.addCell(pesos(item.subtotal()));
        }
        documento.add(tabla);
        documento.add(new Paragraph(" "));
        agregarValor(documento, "Subtotal", factura.getSubtotal());
        agregarValor(documento, "Descuento", factura.getDescuento());
        agregarValor(documento, "Impuesto (" + factura.getTasaImpuesto() + " %)", factura.getImpuesto());
        agregarValor(documento, "Total", factura.getTotal());
        agregarValor(documento, "Saldo", factura.getSaldo());
        documento.close();
        return salida.toByteArray();
    }

    private void agregarValor(Document documento, String nombre, long valor) {
        Paragraph linea = new Paragraph(nombre + ": " + pesos(valor));
        linea.setAlignment(Element.ALIGN_RIGHT);
        documento.add(linea);
    }

    private String pesos(long valor) {
        return "$ " + String.format("%,d", valor);
    }
}
