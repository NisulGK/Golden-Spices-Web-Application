package com.kanishka.demo.wholesale;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.kanishka.demo.user.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class WholesaleReceiptPdfService {

    /* ── Brand colours ── */
    private static final DeviceRgb GOLD       = new DeviceRgb(200, 132,  31);
    private static final DeviceRgb GOLD_DARK  = new DeviceRgb(138,  92,  12);
    private static final DeviceRgb GOLD_LIGHT = new DeviceRgb(253, 247, 238);
    private static final DeviceRgb TEXT_DARK  = new DeviceRgb( 26,  18,   8);
    private static final DeviceRgb TEXT_MUTED = new DeviceRgb(107,  90,  66);
    private static final DeviceRgb GREEN      = new DeviceRgb( 22, 163,  74);
    private static final DeviceRgb GREEN_LIGHT= new DeviceRgb(240, 253, 244);
    private static final DeviceRgb BORDER     = new DeviceRgb(232, 220, 200);
    private static final DeviceRgb BG_LIGHT   = new DeviceRgb(250, 248, 245);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] generate(WholesaleOrder order, User user) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter   writer  = new PdfWriter(baos);
        PdfDocument pdf     = new PdfDocument(writer);
        Document    doc     = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont mono    = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);

        /* ── HEADER BANNER ── */
        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(GOLD_LIGHT)
                .setBorder(new SolidBorder(BORDER, 1f));

        Cell logoCell = new Cell()
                .add(new Paragraph("GOLDEN WHOLESALE").setFont(bold).setFontSize(16).setFontColor(GOLD_DARK))
                .add(new Paragraph("Official Payment Receipt").setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED))
                .add(new Paragraph("Golden Dissanayaka Distributors").setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED))
                .setBorder(Border.NO_BORDER).setPadding(16);
        header.addCell(logoCell);

        // PAID stamp cell
        String dateStr = order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "—";
        String timeStr = order.getCreatedAt() != null ? order.getCreatedAt().format(TIME_FMT) + " LKT" : "—";
        Cell stampCell = new Cell()
                .add(new Paragraph("✓ PAID")
                        .setFont(mono).setFontSize(18).setFontColor(GREEN)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(dateStr).setFont(bold).setFontSize(9).setFontColor(TEXT_DARK).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(timeStr).setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPadding(16);
        header.addCell(stampCell);
        doc.add(header);

        /* ── ORDER REFERENCE BAR ── */
        Table refBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(GOLD)
                .setMarginTop(0);
        refBar.addCell(new Cell()
                .add(new Paragraph("Order #" + order.getId() + "   ·   Wholesale Bulk Order   ·   Card Payment")
                        .setFont(bold).setFontSize(9).setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER).setPaddingTop(6).setPaddingBottom(6));
        doc.add(refBar);

        /* ── BILL TO / RECEIPT DETAILS ── */
        Table details = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(16)
                .setBorder(new SolidBorder(BORDER, 1f));

        // Left — Bill To
        Cell billToHead = new Cell().add(new Paragraph("BILL TO")
                        .setFont(bold).setFontSize(7).setFontColor(TEXT_MUTED))
                .setBorder(Border.NO_BORDER).setPadding(14).setPaddingBottom(4)
                .setBackgroundColor(BG_LIGHT);
        Cell billToBody = new Cell()
                .add(new Paragraph(safe(user != null ? user.getFullName() : null))
                        .setFont(bold).setFontSize(11).setFontColor(TEXT_DARK))
                .add(new Paragraph(safe(user != null ? user.getEmail() : null))
                        .setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED))
                .add(new Paragraph(user != null && user.getPhone() != null ? user.getPhone() : "")
                        .setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED))
                .add(new Paragraph("Wholesale Account")
                        .setFont(bold).setFontSize(8).setFontColor(GOLD_DARK)
                        .setMarginTop(4))
                .setBorder(Border.NO_BORDER).setPadding(14).setPaddingTop(4);

        // Right — Receipt meta
        Cell receiptHead = new Cell().add(new Paragraph("RECEIPT DETAILS")
                        .setFont(bold).setFontSize(7).setFontColor(TEXT_MUTED).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPadding(14).setPaddingBottom(4)
                .setBackgroundColor(BG_LIGHT);
        Cell receiptBody = new Cell()
                .add(labelValue(regular, bold, "Order ID",      "#" + order.getId()))
                .add(labelValue(regular, bold, "Order Status",  order.getStatus()))
                .add(labelValue(regular, bold, "Payment",       "Card Payment — PAID"))
                .add(labelValue(regular, bold, "Date",          dateStr))
                .setBorder(Border.NO_BORDER).setPadding(14).setPaddingTop(4);

        details.addCell(billToHead);
        details.addCell(receiptHead);
        details.addCell(billToBody);
        details.addCell(receiptBody);
        doc.add(details);

        /* ── ITEMS TABLE ── */
        doc.add(new Paragraph("\n").setFontSize(4));
        Table items = new Table(UnitValue.createPercentArray(new float[]{4, 1.2f, 1.5f, 1.5f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(new SolidBorder(BORDER, 1f));

        // Header row
        String[] heads = {"Item Description", "Qty", "Unit Price (LKR)", "Amount (LKR)"};
        for (String h : heads) {
            items.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(TEXT_MUTED))
                    .setBackgroundColor(BG_LIGHT)
                    .setBorderBottom(new SolidBorder(BORDER, 1f))
                    .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                    .setPadding(9));
        }

        // Data row
        String productName = order.getProduct() != null ? order.getProduct().getName() : "—";
        BigDecimal unit  = order.getUnitPrice()   != null ? order.getUnitPrice()   : BigDecimal.ZERO;
        BigDecimal total = order.getTotalAmount()  != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal sub   = unit.multiply(BigDecimal.valueOf(order.getQuantity()));

        items.addCell(dataCellLeft(regular, bold, productName, "Wholesale Bulk Order"));
        items.addCell(dataCell(regular, order.getQuantity() + " units"));
        items.addCell(dataCell(regular, formatLKR(unit)));
        items.addCell(dataCellBold(bold, formatLKR(sub)));
        doc.add(items);

        /* ── TOTALS ── */
        Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(50))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT)
                .setMarginTop(0)
                .setBorder(new SolidBorder(BORDER, 1f));

        totals.addCell(totalLbl(regular, "Subtotal"));
        totals.addCell(totalVal(regular, formatLKR(sub)));

        if (order.getQuantity() > 100) {
            totals.addCell(totalLbl(regular, "Bulk Discount (10% off >100 units)").setFontColor(GREEN));
            totals.addCell(totalVal(regular, "Applied").setFontColor(GREEN));
        }

        // Grand total row
        totals.addCell(new Cell().add(new Paragraph("TOTAL PAID")
                        .setFont(bold).setFontSize(10).setFontColor(TEXT_DARK))
                .setBackgroundColor(GOLD_LIGHT)
                .setBorder(new SolidBorder(BORDER, 1f)).setPadding(10));
        totals.addCell(new Cell().add(new Paragraph("LKR " + formatLKR(total))
                        .setFont(bold).setFontSize(10).setFontColor(GOLD_DARK).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(GOLD_LIGHT)
                .setBorder(new SolidBorder(BORDER, 1f)).setPadding(10));
        doc.add(totals);

        /* ── FOOTER ── */
        doc.add(new Paragraph("\n").setFontSize(6));
        Table footer = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(BG_LIGHT)
                .setBorder(new SolidBorder(BORDER, 1f));
        footer.addCell(new Cell()
                .add(new Paragraph(
                        "This is an official payment receipt for your wholesale purchase with Golden Dissanayaka Distributors. "
                                + "Please retain for your records. For queries contact: 071 857 9984 / 077 780 8259")
                        .setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Thank you for your order, " + safe(user != null ? user.getFullName() : "Valued Customer") + "!")
                        .setFont(bold).setFontSize(9).setFontColor(GOLD_DARK)
                        .setTextAlignment(TextAlignment.CENTER).setMarginTop(6))
                .setBorder(Border.NO_BORDER).setPadding(16));
        doc.add(footer);

        doc.close();
        return baos.toByteArray();
    }

    /* ── helpers ── */
    private String safe(String s) { return s != null ? s : "—"; }

    private String formatLKR(BigDecimal v) {
        return String.format("%,.2f", v);
    }

    private Paragraph labelValue(PdfFont regular, PdfFont bold, String lbl, String val) {
        return new Paragraph()
                .add(new Text(lbl + ":  ").setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED))
                .add(new Text(val).setFont(bold).setFontSize(8).setFontColor(TEXT_DARK))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(3);
    }

    private Cell dataCellLeft(PdfFont regular, PdfFont bold, String name, String sub) {
        return new Cell()
                .add(new Paragraph(name).setFont(bold).setFontSize(9).setFontColor(TEXT_DARK))
                .add(new Paragraph(sub).setFont(regular).setFontSize(7).setFontColor(TEXT_MUTED))
                .setBorder(Border.NO_BORDER).setPadding(10);
    }

    private Cell dataCell(PdfFont regular, String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(regular).setFontSize(9)
                        .setFontColor(TEXT_MUTED).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell dataCellBold(PdfFont bold, String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(bold).setFontSize(9)
                        .setFontColor(TEXT_DARK).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell totalLbl(PdfFont font, String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(8).setFontColor(TEXT_MUTED))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER, .5f))
                .setPadding(7);
    }

    private Cell totalVal(PdfFont font, String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(8)
                        .setFontColor(TEXT_DARK).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER, .5f))
                .setPadding(7);
    }
}