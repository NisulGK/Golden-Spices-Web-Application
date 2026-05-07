package com.kanishka.demo.cart;

import com.kanishka.demo.catalog.Product;
import com.kanishka.demo.catalog.ProductRepository;
import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // =========================================================
    // GET OR CREATE CART
    // =========================================================
    public Cart getOrCreateCart(String userEmail) {
        return cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    // =========================================================
    // ADD
    // =========================================================
    public void add(String userEmail, Long productId, int qty) {
        if (qty <= 0) throw new RuntimeException("Invalid quantity selected.");

        Cart cart = getOrCreateCart(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (product.getStockQty() == null || product.getStockQty() <= 0) {
            throw new RuntimeException("Product is out of stock.");
        }

        CartItemEntity existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(
                    Math.min(existing.getQuantity() + qty, product.getStockQty()));
            cartItemRepository.save(existing);
            return;
        }

        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(Math.min(qty, product.getStockQty()));
        cartItemRepository.save(item);
    }

    // =========================================================
    // UPDATE QUANTITY
    // =========================================================
    public void updateQuantity(String userEmail, Long productId, int qty) {
        Cart cart = getOrCreateCart(userEmail);

        CartItemEntity item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Cart item not found."));

        Product product = item.getProduct();
        if (product == null || product.getStockQty() == null || product.getStockQty() <= 0) {
            cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
            entityManager.flush();
            throw new RuntimeException("Product is out of stock.");
        }
        if (qty <= 0) {
            cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
            entityManager.flush();
            return;
        }
        item.setQuantity(Math.min(qty, product.getStockQty()));
        cartItemRepository.save(item);
    }

    // =========================================================
    // REMOVE — direct JPQL DELETE bypasses first-level cache
    // =========================================================
    public void remove(String userEmail, Long productId) {
        Cart cart = getOrCreateCart(userEmail);

        // Direct JPQL DELETE — skips entity cache, writes straight to DB
        int deleted = cartItemRepository
                .deleteByCartIdAndProductId(cart.getId(), productId);

        // Flush immediately and clear the first-level (L1) cache so the
        // next getCartByUser() reloads fresh data from DB
        entityManager.flush();
        entityManager.clear();

        if (deleted == 0) {
            throw new RuntimeException("Item not found in cart.");
        }
    }

    // =========================================================
    // CLEAR — direct JPQL DELETE + cache eviction
    // =========================================================
    public void clear(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        cartItemRepository.deleteByCartId(cart.getId());
        entityManager.flush();
        entityManager.clear();
    }

    // =========================================================
    // GET CART (read-only — always fresh from DB)
    // =========================================================
    @Transactional(readOnly = true)
    public Cart getCartByUser(String userEmail) {
        // Clear L1 cache before read so we always get fresh DB state
        entityManager.clear();
        return cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    // =========================================================
    // VALIDATE CART
    // =========================================================
    public void validateCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        for (CartItemEntity item : cart.getItems()) {
            Product product = item.getProduct();
            if (product == null || product.getStockQty() == null || product.getStockQty() <= 0) {
                cartItemRepository.deleteByCartIdAndProductId(cart.getId(), item.getProduct().getId());
            } else if (item.getQuantity() > product.getStockQty()) {
                item.setQuantity(product.getStockQty());
                cartItemRepository.save(item);
            }
        }
        entityManager.flush();
        entityManager.clear();
    }
}