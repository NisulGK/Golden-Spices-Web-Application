package com.kanishka.demo.cart;

import com.kanishka.demo.catalog.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "cart_items")
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    public Long getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity <= 0 ? 1 : quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(quantity, 1);
    }

    public BigDecimal getSubtotal() {
        if (product == null || product.getPriceLkr() == null) {
            return BigDecimal.ZERO;
        }

        return product.getPriceLkr()
                .multiply(BigDecimal.valueOf(getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalPrice() {
        return getSubtotal();
    }

    public boolean isOutOfStock() {
        return product == null
                || product.getStockQty() == null
                || product.getStockQty() <= 0;
    }

    public boolean exceedsStock() {
        if (product == null || product.getStockQty() == null) {
            return false;
        }
        return getQuantity() > product.getStockQty();
    }

    public boolean isLowStock() {
        if (product == null || product.getStockQty() == null) {
            return false;
        }
        return product.getStockQty() > 0 && product.getStockQty() <= 5;
    }

    // =========================================
    // HELPER METHODS FOR THYMELEAF
    // =========================================

    public Long productId() {
        return product != null ? product.getId() : null;
    }

    public String name() {
        return product != null ? product.getName() : "";
    }

    public String brand() {
        return product != null ? product.getBrand() : "";
    }

    public String size() {
        return product != null ? product.getSizeLabel() : "";
    }

    public BigDecimal price() {
        return (product != null && product.getPriceLkr() != null)
                ? product.getPriceLkr()
                : BigDecimal.ZERO;
    }

    public int qty() {
        return getQuantity();
    }

    public int stockQty() {
        return (product != null && product.getStockQty() != null)
                ? product.getStockQty()
                : 0;
    }

    public String getDisplayName() {
        String safeBrand = brand() != null ? brand() : "";
        String safeName = name() != null ? name() : "";
        String safeSize = (size() != null && !size().isBlank()) ? " (" + size() + ")" : "";

        return (safeBrand + " " + safeName + safeSize).trim();
    }

    public String getSafeImage() {
        if (product == null || product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            return "/images/products/default-product.png";
        }

        String imageUrl = product.getImageUrl();

        if (imageUrl.startsWith("/images/")) {
            return imageUrl;
        }

        return "/images/products/" + imageUrl;
    }

    public int getSafeQty() {
        int qty = getQuantity();
        int stock = stockQty();

        if (qty <= 0) return 1;
        if (stock > 0 && qty > stock) return stock;
        return qty;
    }
}