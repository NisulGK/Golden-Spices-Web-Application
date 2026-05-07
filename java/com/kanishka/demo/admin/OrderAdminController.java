package com.kanishka.demo.admin;

import com.kanishka.demo.Order.*;
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
import com.itextpdf.io.font.constants.StandardFonts;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderRepository        orderRepository;
    private final OrderService           orderService;
    private final OrderTrackingRepository trackingRepository;

    private static final DeviceRgb GOLD    = new DeviceRgb(200, 132, 31);
    private static final DeviceRgb DARK    = new DeviceRgb(17,  24,  39);
    private static final DeviceRgb GRAY    = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb LGRAY   = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb BGLIGHT = new DeviceRgb(249, 250, 251);

    // =========================================================
    // LIST
    // =========================================================
    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status,
                       @RequestParam(required = false) String orderNumber,
                       Model model) {

        List<Order> orders;

        if (orderNumber != null && !orderNumber.isBlank()) {
            orders = orderRepository.findByOrderNumber(orderNumber)
                    .map(java.util.List::of)
                    .orElse(java.util.List.of());
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
            model.addAttribute("filterStatus", status);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        // ✅ FIX: build itemCounts map so admin/orders/list.html can render item counts
        Map<Long, Integer> itemCounts = orders.stream()
                .collect(Collectors.toMap(
                        Order::getId,
                        Order::getTotalItemCount));

        model.addAttribute("orders",    orders);
        model.addAttribute("itemCounts", itemCounts);
        model.addAttribute("statuses",  OrderStatus.values());
        return "admin/orders/list";
    }

    // =========================================================
    // DETAIL
    // =========================================================
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        List<OrderTracking> trackingEvents =
                trackingRepository.findByOrderIdOrderByCreatedAtAsc(id);

        model.addAttribute("order",          order);
        model.addAttribute("statuses",       OrderStatus.values());
        model.addAttribute("trackingEvents", trackingEvents);
        return "admin/orders/detail";
    }

    // =========================================================
    // UPDATE STATUS  (also auto-adds tracking event)
    // =========================================================
    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus newStatus,
                               RedirectAttributes ra) {
        orderService.updateOrderStatus(id, newStatus);
        ra.addFlashAttribute("success",
                "Order status updated to " + newStatus.name() + ".");
        return "redirect:/admin/orders/" + id;
    }

    // =========================================================
    // ADD MANUAL TRACKING NOTE
    // =========================================================
    @PostMapping("/{id}/tracking")
    public String addTrackingNote(@PathVariable Long id,
                                  @RequestParam String note,
                                  RedirectAttributes ra) {

        if (note == null || note.trim().isBlank()) {
            ra.addFlashAttribute("error", "Tracking note cannot be empty.");
            return "redirect:/admin/orders/" + id;
        }

        Order order = orderService.getOrderById(id);

        OrderTracking event = OrderTracking.builder()
                .order(order)
                .status(order.getStatus())
                .title("Admin Note")
                .description(note.trim())
                .active(true)
                .build();

        trackingRepository.save(event);
        ra.addFlashAttribute("success", "Tracking note added.");
        return "redirect:/admin/orders/" + id;
    }

    // =========================================================
    // EXPORT PDF
    // =========================================================
    @GetMapping("/{id}/export")
    public void exportPdf(@PathVariable Long id,
                          HttpServletResponse response) throws IOException {

        Order order = orderService.getOrderById(id);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=Invoice-" + order.getOrderNumber() + ".pdf");

        PdfWriter   writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 44, 36, 44);

        PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Top gold bar
        Table topBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
        topBar.addCell(new Cell().setHeight(6)
                .setBackgroundColor(GOLD).setBorder(Border.NO_BORDER));
        doc.add(topBar);

        // Header
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

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
        rightCell.add(new Paragraph("INVOICE")
                .setFont(bold).setFontSize(24).setFontColor(GOLD));
        rightCell.add(new Paragraph(order.getOrderNumber())
                .setFont(bold).setFontSize(9).setFontColor(DARK).setMarginTop(4));
        if (order.getCreatedAt() != null) {
            rightCell.add(new Paragraph(order.getCreatedAt().toLocalDate().toString())
                    .setFont(normal).setFontSize(8).setFontColor(GRAY));
        }
        header.addCell(rightCell);
        doc.add(header);

        // Divider
        doc.add(new LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                .setStrokeColor(LGRAY).setMarginBottom(16));

        // Bill To + Order Info
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(24);

        Cell billCell = new Cell().setBorder(Border.NO_BORDER);
        billCell.add(new Paragraph("BILL TO")
                .setFont(bold).setFontSize(7).setFontColor(GRAY)
                .setCharacterSpacing(1.2f).setMarginBottom(6));
        billCell.add(new Paragraph(order.getCustomerName())
                .setFont(bold).setFontSize(11).setFontColor(DARK));
        if (order.getCustomerEmail() != null)
            billCell.add(new Paragraph(order.getCustomerEmail())
                    .setFont(normal).setFontSize(8.5f).setFontColor(GRAY));
        if (order.getCustomerPhone() != null)
            billCell.add(new Paragraph(order.getCustomerPhone())
                    .setFont(normal).setFontSize(8.5f).setFontColor(GRAY));
        if (order.getDeliveryAddress() != null)
            billCell.add(new Paragraph(order.getDeliveryAddress())
                    .setFont(normal).setFontSize(8.5f).setFontColor(GRAY));
        infoTable.addCell(billCell);

        Cell orderInfoCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        orderInfoCell.add(new Paragraph("ORDER INFO")
                .setFont(bold).setFontSize(7).setFontColor(GRAY)
                .setCharacterSpacing(1.2f).setMarginBottom(6));
        orderInfoCell.add(new Paragraph("Status:   " + order.getStatus())
                .setFont(normal).setFontSize(9).setFontColor(DARK));
        orderInfoCell.add(new Paragraph("Payment: " + order.getPaymentStatus())
                .setFont(normal).setFontSize(9).setFontColor(DARK));
        infoTable.addCell(orderInfoCell);
        doc.add(infoTable);

        // Items
        doc.add(new Paragraph("ORDER ITEMS")
                .setFont(bold).setFontSize(7).setFontColor(GRAY)
                .setCharacterSpacing(1.2f).setMarginBottom(8));

        Table items = new Table(
                UnitValue.createPercentArray(new float[]{8, 30, 15, 18, 10, 19}))
                .setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"#", "Product", "Size", "Unit Price", "Qty", "Subtotal"};
        for (String h : headers) {
            items.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(7.5f).setFontColor(GRAY))
                    .setBackgroundColor(BGLIGHT)
                    .setBorderTop(new SolidBorder(LGRAY, 0.5f))
                    .setBorderBottom(new SolidBorder(LGRAY, 0.5f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setPadding(7));
        }

        int idx = 1;
        for (OrderItem item : order.getItems()) {
            String[] row = {
                    String.valueOf(idx++),
                    item.getProductName(),
                    item.getProductSize(),
                    "LKR " + item.getUnitPrice(),
                    String.valueOf(item.getQuantity()),
                    "LKR " + item.getSubtotal()
            };
            TextAlignment[] aligns = {
                    TextAlignment.CENTER, TextAlignment.LEFT, TextAlignment.LEFT,
                    TextAlignment.RIGHT, TextAlignment.CENTER, TextAlignment.RIGHT
            };
            for (int i = 0; i < row.length; i++) {
                items.addCell(new Cell()
                        .add(new Paragraph(row[i])
                                .setFont(i == 5 ? bold : normal)
                                .setFontSize(9).setFontColor(DARK)
                                .setTextAlignment(aligns[i]))
                        .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                        .setBorderTop(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(LGRAY, 0.3f))
                        .setPadding(7));
            }
        }
        doc.add(items);

        // Total
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(10);
        Cell totalCell = new Cell().setBackgroundColor(BGLIGHT)
                .setBorder(new SolidBorder(LGRAY, 0.5f)).setPadding(10);
        Table totalInner = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));
        totalInner.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Total Amount")
                        .setFont(bold).setFontSize(10).setFontColor(DARK)));
        totalInner.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("LKR " + order.getTotalAmount())
                        .setFont(bold).setFontSize(13).setFontColor(GOLD)
                        .setTextAlignment(TextAlignment.RIGHT)));
        totalCell.add(totalInner);
        totalTable.addCell(totalCell);
        doc.add(totalTable);

        // Footer
        doc.add(new LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.3f))
                .setStrokeColor(LGRAY).setMarginTop(30).setMarginBottom(8));
        doc.add(new Paragraph(
                "Thank you for your business · Golden Dissanayaka Distributors")
                .setFont(normal).setFontSize(8).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        Table bottomBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(12);
        bottomBar.addCell(new Cell().setHeight(6)
                .setBackgroundColor(GOLD).setBorder(Border.NO_BORDER));
        doc.add(bottomBar);

        doc.close();
    }
}