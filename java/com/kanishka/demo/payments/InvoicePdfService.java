package com.kanishka.demo.payments;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kanishka.demo.Order.Order;
import com.kanishka.demo.Order.OrderItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class InvoicePdfService {

    private static final DeviceRgb GOLD = new DeviceRgb(200, 132, 31);
    private static final DeviceRgb DARK = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb GRAY = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb LGRAY = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb BGLIGHT = new DeviceRgb(249, 250, 251);

    public byte[] generateInvoice(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(36, 44, 36, 44);

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            Table topBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);
            topBar.addCell(new Cell().setHeight(6)
                    .setBackgroundColor(GOLD)
                    .setBorder(Border.NO_BORDER));
            doc.add(topBar);

            Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

            Cell leftCell = new Cell().setBorder(Border.NO_BORDER);
            leftCell.add(new Paragraph("GOLDEN")
                    .setFont(bold).setFontSize(20).setFontColor(DARK));
            leftCell.add(new Paragraph("Dissanayaka Distributors")
                    .setFont(normal).setFontSize(9).setFontColor(GRAY).setMarginTop(2));
            leftCell.add(new Paragraph("968/2, Old Kesbewa Road, Nugegoda")
                    .setFont(normal).setFontSize(8).setFontColor(GRAY));
            leftCell.add(new Paragraph("Tel: 077 780 8259  ·  071 857 9984")
                    .setFont(normal).setFontSize(8).setFontColor(GRAY));
            header.addCell(leftCell);

            Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            rightCell.add(new Paragraph("RECEIPT")
                    .setFont(bold).setFontSize(24).setFontColor(GOLD));
            rightCell.add(new Paragraph(order.getOrderNumber())
                    .setFont(bold).setFontSize(9).setFontColor(DARK).setMarginTop(4));
            if (order.getCreatedAt() != null) {
                rightCell.add(new Paragraph(order.getCreatedAt().toLocalDate().toString())
                        .setFont(normal).setFontSize(8).setFontColor(GRAY));
            }
            header.addCell(rightCell);
            doc.add(header);

            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                    .setStrokeColor(LGRAY)
                    .setMarginBottom(16));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(24);

            Cell billCell = new Cell().setBorder(Border.NO_BORDER);
            billCell.add(new Paragraph("BILL TO")
                    .setFont(bold).setFontSize(7).setFontColor(GRAY)
                    .setCharacterSpacing(1.2f).setMarginBottom(6));
            billCell.add(new Paragraph(order.getCustomerName())
                    .setFont(bold).setFontSize(11).setFontColor(DARK));
            if (order.getCustomerEmail() != null) {
                billCell.add(new Paragraph(order.getCustomerEmail())
                        .setFont(normal).setFontSize(8.5f).setFontColor(GRAY));
            }
            if (order.getCustomerPhone() != null) {
                billCell.add(new Paragraph(order.getCustomerPhone())
                        .setFont(normal).setFontSize(8.5f).setFontColor(GRAY));
            }
            if (order.getDeliveryAddress() != null) {
                billCell.add(new Paragraph(order.getDeliveryAddress())
                        .setFont(normal).setFontSize(8.5f).setFontColor(GRAY));
            }
            infoTable.addCell(billCell);

            Cell orderInfoCell = new Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            orderInfoCell.add(new Paragraph("ORDER INFO")
                    .setFont(bold).setFontSize(7).setFontColor(GRAY)
                    .setCharacterSpacing(1.2f).setMarginBottom(6));
            orderInfoCell.add(new Paragraph("Status: " + order.getStatus())
                    .setFont(normal).setFontSize(9).setFontColor(DARK));
            orderInfoCell.add(new Paragraph("Payment: " + order.getPaymentStatus())
                    .setFont(normal).setFontSize(9).setFontColor(DARK));
            infoTable.addCell(orderInfoCell);

            doc.add(infoTable);

            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4.5f, 2f, 2f, 1.5f, 2f}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

            addHeaderCell(itemsTable, "Product", bold);
            addHeaderCell(itemsTable, "Size", bold);
            addHeaderCell(itemsTable, "Unit Price", bold);
            addHeaderCell(itemsTable, "Qty", bold);
            addHeaderCell(itemsTable, "Subtotal", bold);

            for (OrderItem item : order.getItems()) {
                addBodyCell(itemsTable, item.getProductName(), normal, TextAlignment.LEFT);
                addBodyCell(itemsTable, item.getProductSize(), normal, TextAlignment.LEFT);
                addBodyCell(itemsTable, "LKR " + formatMoney(item.getUnitPrice()), normal, TextAlignment.RIGHT);
                addBodyCell(itemsTable, String.valueOf(item.getQuantity()), normal, TextAlignment.CENTER);
                addBodyCell(itemsTable, "LKR " + formatMoney(item.getSubtotal()), normal, TextAlignment.RIGHT);
            }

            doc.add(itemsTable);

            Table totalTable = new Table(UnitValue.createPercentArray(new float[]{75, 25}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);
            totalTable.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("Total Amount")
                            .setFont(bold)
                            .setFontSize(11)
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setFontColor(DARK)));
            totalTable.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("LKR " + formatMoney(order.getTotalAmount()))
                            .setFont(bold)
                            .setFontSize(12)
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setFontColor(GOLD)));
            doc.add(totalTable);

            if (order.getNotes() != null && !order.getNotes().isBlank()) {
                doc.add(new Paragraph("Customer Notes")
                        .setFont(bold).setFontSize(10).setFontColor(DARK).setMarginBottom(6));
                doc.add(new Paragraph(order.getNotes())
                        .setFont(normal).setFontSize(9).setFontColor(GRAY).setMarginBottom(16));
            }

            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                    .setStrokeColor(LGRAY)
                    .setMarginBottom(12));

            doc.add(new Paragraph("Thank you for shopping with GOLDEN.")
                    .setFont(bold)
                    .setFontSize(10)
                    .setFontColor(DARK)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("This is a computer-generated receipt.")
                    .setFont(normal)
                    .setFontSize(8)
                    .setFontColor(GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void addHeaderCell(Table table, String text, PdfFont font) {
        table.addCell(new Cell()
                .setBackgroundColor(BGLIGHT)
                .setBorder(new SolidBorder(LGRAY, 1))
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(DARK)));
    }

    private void addBodyCell(Table table, String text, PdfFont font, TextAlignment align) {
        table.addCell(new Cell()
                .setBorder(new SolidBorder(LGRAY, 1))
                .add(new Paragraph(text == null ? "" : text)
                        .setFont(font)
                        .setFontSize(8.5f)
                        .setFontColor(GRAY)
                        .setTextAlignment(align)));
    }

    private String formatMoney(java.math.BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return String.format("%,.2f", value);
    }
}