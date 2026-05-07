package com.kanishka.demo.cart;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CartItem(
        Long productId,
        String name,
        String brand,
        String size,
        BigDecimal price,
        int qty,
        int stockQty,     // ✅ ADD THIS FIELD
        String imageUrl
) {

    // ===============================
    // SAFE SUBTOTAL
    // ===============================
    public BigDecimal getSubtotal() {

        BigDecimal safePrice =
                price != null ? price : BigDecimal.ZERO;

        int safeQty = getSafeQty();

        return safePrice
                .multiply(BigDecimal.valueOf(safeQty))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ===============================
    // SAFE DISPLAY NAME
    // ===============================
    public String getDisplayName() {

        String safeBrand =
                brand != null ? brand : "";

        String safeName =
                name != null ? name : "";

        String safeSize =
                size != null ? " (" + size + ")" : "";

        return (safeBrand + " " + safeName + safeSize).trim();
    }

    // ===============================
    // SAFE IMAGE
    // ===============================
    public String getSafeImage() {

        if (imageUrl == null || imageUrl.isBlank()) {
            return "/images/products/default-product.png";
        }

        if (imageUrl.startsWith("/images/")) {
            return imageUrl;
        }

        return "/images/products/" + imageUrl;
    }

    // ===============================
    // SAFE QTY
    // ===============================
    public int getSafeQty() {
        if (qty <= 0) return 1;
        if (qty > stockQty) return stockQty;
        return qty;
    }

    // ===============================
    // STOCK CHECK
    // ===============================
    public boolean isOutOfStock() {
        return stockQty <= 0;
    }

    public boolean isLowStock() {
        return stockQty > 0 && stockQty <= 5;
    }

    public boolean exceedsStock() {
        return qty > stockQty;
    }
}
