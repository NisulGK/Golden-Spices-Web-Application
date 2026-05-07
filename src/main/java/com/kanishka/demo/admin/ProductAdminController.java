package com.kanishka.demo.admin;

import com.kanishka.demo.catalog.Product;
import com.kanishka.demo.catalog.ProductRepository;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductRepository productRepository;

    // ── iText PDF colours (matching the invoice style) ──
    private static final DeviceRgb GOLD    = new DeviceRgb(200, 132,  31);
    private static final DeviceRgb DARK    = new DeviceRgb( 17,  24,  39);
    private static final DeviceRgb GRAY    = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb LGRAY   = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb BGLIGHT = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb GREEN   = new DeviceRgb( 22, 163,  74);
    private static final DeviceRgb RED     = new DeviceRgb(220,  38,  38);
    private static final DeviceRgb AMBER   = new DeviceRgb(217, 119,   6);

    // =========================================================
    // LIST  –  now includes analytics attributes
    // =========================================================
    @GetMapping
    public String list(Model model) {
        List<Product> products = productRepository.findAll();

        // ── Analytics ──
        long totalProducts  = products.size();
        long activeProducts = products.stream().filter(p -> Boolean.TRUE.equals(p.getActive())).count();
        long inactiveProducts = totalProducts - activeProducts;
        long outOfStock     = products.stream().filter(p -> p.getStockQty() != null && p.getStockQty() == 0).count();
        long lowStock       = products.stream().filter(p -> p.getStockQty() != null && p.getStockQty() > 0 && p.getStockQty() <= 5).count();
        long healthyStock   = products.stream().filter(p -> p.getStockQty() != null && p.getStockQty() > 5).count();

        BigDecimal totalInventoryValue = products.stream()
                .filter(p -> p.getPriceLkr() != null && p.getStockQty() != null)
                .map(p -> p.getPriceLkr().multiply(BigDecimal.valueOf(p.getStockQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // brand breakdown  (name → count)
        Map<String, Long> byBrand = products.stream()
                .collect(Collectors.groupingBy(Product::getBrand, Collectors.counting()));

        // top 5 most-stocked products
        List<Product> topStocked = products.stream()
                .filter(p -> p.getStockQty() != null)
                .sorted((a, b) -> Integer.compare(b.getStockQty(), a.getStockQty()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("products",            products);
        model.addAttribute("totalProducts",       totalProducts);
        model.addAttribute("activeProducts",      activeProducts);
        model.addAttribute("inactiveProducts",    inactiveProducts);
        model.addAttribute("outOfStock",          outOfStock);
        model.addAttribute("lowStock",            lowStock);
        model.addAttribute("healthyStock",        healthyStock);
        model.addAttribute("totalInventoryValue", totalInventoryValue);
        model.addAttribute("byBrand",             byBrand);
        model.addAttribute("topStocked",          topStocked);

        return "admin/products/list";
    }

    // =========================================================
    // CREATE FORM
    // =========================================================
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        return "admin/products/form";
    }

    @PostMapping("/new")
    public String create(
            @RequestParam String brand,
            @RequestParam String name,
            @RequestParam String sizeLabel,
            @RequestParam BigDecimal priceLkr,
            @RequestParam(defaultValue = "0") Integer stockQty,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(defaultValue = "true") Boolean active,
            RedirectAttributes ra) {

        String error = validateProduct(brand, name, sizeLabel, priceLkr, stockQty);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/admin/products/new";
        }

        Product product = Product.builder()
                .brand(brand.trim())
                .name(name.trim())
                .sizeLabel(sizeLabel.trim())
                .priceLkr(priceLkr)
                .stockQty(stockQty)
                .description(description != null ? description.trim() : null)
                .imageUrl(imageUrl != null ? imageUrl.trim() : null)
                .active(active)
                .build();

        productRepository.save(product);
        ra.addFlashAttribute("success", "Product \"" + name + "\" created successfully!");
        return "redirect:/admin/products";
    }

    // =========================================================
    // EDIT FORM
    // =========================================================
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        model.addAttribute("product", product);
        return "admin/products/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @RequestParam String brand,
            @RequestParam String name,
            @RequestParam String sizeLabel,
            @RequestParam BigDecimal priceLkr,
            @RequestParam Integer stockQty,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(defaultValue = "true") Boolean active,
            RedirectAttributes ra) {

        String error = validateProduct(brand, name, sizeLabel, priceLkr, stockQty);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/admin/products/" + id + "/edit";
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setBrand(brand.trim());
        product.setName(name.trim());
        product.setSizeLabel(sizeLabel.trim());
        product.setPriceLkr(priceLkr);
        product.setStockQty(stockQty);
        product.setDescription(description != null ? description.trim() : null);
        product.setImageUrl(imageUrl != null ? imageUrl.trim() : null);
        product.setActive(active);

        productRepository.save(product);
        ra.addFlashAttribute("success", "Product updated successfully!");
        return "redirect:/admin/products";
    }

    // =========================================================
    // DELETE
    // =========================================================
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        if (!productRepository.existsById(id)) {
            ra.addFlashAttribute("error", "Product not found.");
            return "redirect:/admin/products";
        }
        productRepository.deleteById(id);
        ra.addFlashAttribute("success", "Product deleted successfully!");
        return "redirect:/admin/products";
    }

    // =========================================================
    // TOGGLE ACTIVE
    // =========================================================
    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(!product.getActive());
        productRepository.save(product);
        String status = product.getActive() ? "activated" : "deactivated";
        ra.addFlashAttribute("success", "Product " + status + " successfully!");
        return "redirect:/admin/products";
    }

    // =========================================================
    // EXPORT ALL PRODUCTS AS PDF
    // =========================================================
    @GetMapping("/export-pdf")
    public void exportPdf(HttpServletResponse response) throws IOException {

        List<Product> products = productRepository.findAll();

        response.setContentType("application/pdf");
        String filename = "Products-Report-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        PdfWriter   writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf, PageSize.A4.rotate()); // landscape for more columns
        doc.setMargins(30, 36, 30, 36);

        PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ── Top gold bar ──
        Table topBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);
        topBar.addCell(new Cell().setHeight(5)
                .setBackgroundColor(GOLD).setBorder(Border.NO_BORDER));
        doc.add(topBar);

        // ── Header ──
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(14);

        Cell left = new Cell().setBorder(Border.NO_BORDER);
        left.add(new Paragraph("GOLDEN").setFont(bold).setFontSize(18).setFontColor(DARK));
        left.add(new Paragraph("Dissanayaka Distributors")
                .setFont(normal).setFontSize(8).setFontColor(GRAY));
        left.add(new Paragraph("968/2, Old Kesbewa Road, Nugegoda")
                .setFont(normal).setFontSize(7.5f).setFontColor(GRAY));
        left.add(new Paragraph("Tel: 077 780 8259  ·  071 857 9984")
                .setFont(normal).setFontSize(7.5f).setFontColor(GRAY));
        header.addCell(left);

        Cell right = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph("PRODUCT CATALOGUE")
                .setFont(bold).setFontSize(20).setFontColor(GOLD));
        right.add(new Paragraph("Generated: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")))
                .setFont(normal).setFontSize(8).setFontColor(GRAY).setMarginTop(4));
        right.add(new Paragraph("Total Products: " + products.size())
                .setFont(bold).setFontSize(9).setFontColor(DARK));
        header.addCell(right);
        doc.add(header);

        // ── Divider ──
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                .setStrokeColor(LGRAY).setMarginBottom(12));

        // ── Summary boxes ──
        long activeCount  = products.stream().filter(p -> Boolean.TRUE.equals(p.getActive())).count();
        long oos          = products.stream().filter(p -> p.getStockQty() != null && p.getStockQty() == 0).count();
        long low          = products.stream().filter(p -> p.getStockQty() != null && p.getStockQty() > 0 && p.getStockQty() <= 5).count();
        BigDecimal invVal = products.stream()
                .filter(p -> p.getPriceLkr() != null && p.getStockQty() != null)
                .map(p -> p.getPriceLkr().multiply(BigDecimal.valueOf(p.getStockQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);

        addSummaryCell(summary, bold, normal, "Total Products", String.valueOf(products.size()), DARK);
        addSummaryCell(summary, bold, normal, "Active",         String.valueOf(activeCount),     GREEN);
        addSummaryCell(summary, bold, normal, "Out of Stock",   String.valueOf(oos),             RED);
        addSummaryCell(summary, bold, normal, "Inventory Value","LKR " + String.format("%,.2f", invVal), GOLD);
        doc.add(summary);

        // ── Products table ──
        doc.add(new Paragraph("PRODUCT LIST")
                .setFont(bold).setFontSize(7).setFontColor(GRAY)
                .setCharacterSpacing(1.2f).setMarginBottom(6));

        Table table = new Table(
                UnitValue.createPercentArray(new float[]{4, 18, 22, 9, 13, 8, 12, 10, 10}))
                .setWidth(UnitValue.createPercentValue(100));

        String[] cols = {"#", "Brand", "Product Name", "Size", "Price (LKR)", "Stock", "Inv. Value (LKR)", "Status", "Active"};
        for (String col : cols) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(col).setFont(bold).setFontSize(7).setFontColor(GRAY))
                    .setBackgroundColor(BGLIGHT)
                    .setBorderTop(new SolidBorder(LGRAY, 0.5f))
                    .setBorderBottom(new SolidBorder(LGRAY, 0.5f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setPadding(5));
        }

        int idx = 1;
        for (Product p : products) {
            BigDecimal lineVal = (p.getPriceLkr() != null && p.getStockQty() != null)
                    ? p.getPriceLkr().multiply(BigDecimal.valueOf(p.getStockQty()))
                    : BigDecimal.ZERO;

            String stockStatus;
            DeviceRgb statusColor;
            if (p.getStockQty() == null || p.getStockQty() == 0) {
                stockStatus = "Out of Stock"; statusColor = RED;
            } else if (p.getStockQty() <= 5) {
                stockStatus = "Low Stock"; statusColor = AMBER;
            } else {
                stockStatus = "In Stock"; statusColor = GREEN;
            }

            Object[][] cells = {
                    {String.valueOf(idx++),          DARK,        TextAlignment.CENTER},
                    {p.getBrand(),                   DARK,        TextAlignment.LEFT},
                    {p.getName(),                    DARK,        TextAlignment.LEFT},
                    {p.getSizeLabel(),               GRAY,        TextAlignment.CENTER},
                    {p.getPriceLkr() != null ? String.format("%,.2f", p.getPriceLkr()) : "—",
                            DARK,        TextAlignment.RIGHT},
                    {p.getStockQty() != null ? String.valueOf(p.getStockQty()) : "0",
                            DARK,        TextAlignment.CENTER},
                    {String.format("%,.2f", lineVal), DARK,       TextAlignment.RIGHT},
                    {stockStatus,                    statusColor, TextAlignment.CENTER},
                    {Boolean.TRUE.equals(p.getActive()) ? "Active" : "Inactive",
                            Boolean.TRUE.equals(p.getActive()) ? GREEN : GRAY,
                            TextAlignment.CENTER},
            };

            boolean alt = idx % 2 == 0;
            for (Object[] cell : cells) {
                Cell c = new Cell()
                        .add(new Paragraph((String) cell[0])
                                .setFont(normal).setFontSize(7.5f)
                                .setFontColor((DeviceRgb) cell[1])
                                .setTextAlignment((TextAlignment) cell[2]))
                        .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                        .setBorderTop(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(LGRAY, 0.3f))
                        .setPadding(5);
                if (alt) c.setBackgroundColor(new DeviceRgb(252, 252, 253));
                table.addCell(c);
            }
        }
        doc.add(table);

        // ── Footer ──
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.3f))
                .setStrokeColor(LGRAY).setMarginTop(20).setMarginBottom(6));
        doc.add(new Paragraph(
                "Golden Dissanayaka Distributors  ·  Product Catalogue  ·  Confidential")
                .setFont(normal).setFontSize(7).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        Table bottomBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(8);
        bottomBar.addCell(new Cell().setHeight(5)
                .setBackgroundColor(GOLD).setBorder(Border.NO_BORDER));
        doc.add(bottomBar);

        doc.close();
    }

    // ── helper: summary box cell ──
    private void addSummaryCell(Table t, PdfFont bold, PdfFont normal,
                                String label, String value, DeviceRgb color) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(LGRAY, 0.5f))
                .setBackgroundColor(BGLIGHT)
                .setPadding(8);
        cell.add(new Paragraph(label)
                .setFont(normal).setFontSize(7).setFontColor(GRAY).setMarginBottom(3));
        cell.add(new Paragraph(value)
                .setFont(bold).setFontSize(13).setFontColor(color));
        t.addCell(cell);
    }

    // ── Shared validation ──
    private String validateProduct(String brand, String name, String sizeLabel,
                                   BigDecimal priceLkr, Integer stockQty) {
        if (brand == null || brand.trim().isBlank())
            return "Brand is required.";
        if (name == null || name.trim().isBlank())
            return "Product name is required.";
        if (sizeLabel == null || sizeLabel.trim().isBlank())
            return "Size label is required.";
        if (priceLkr == null || priceLkr.compareTo(BigDecimal.ZERO) <= 0)
            return "Price must be greater than zero.";
        if (stockQty == null || stockQty < 0)
            return "Stock quantity cannot be negative.";
        return null;
    }
}