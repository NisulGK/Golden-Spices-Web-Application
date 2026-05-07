package com.kanishka.demo.cart;

import com.kanishka.demo.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER so items are always loaded with the cart in one query.
    // After entityManager.clear(), the next findByUserEmail triggers
    // a fresh SELECT with JOIN FETCH from CartRepository.
    @OneToMany(mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<CartItemEntity> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void addItem(CartItemEntity item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItemEntity item) {
        items.remove(item);
        item.setCart(null);
    }

    public void clearCart() {
        for (CartItemEntity i : items) i.setCart(null);
        items.clear();
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId()                              { return id; }
    public List<CartItemEntity> getItems()           { return items; }
    public void setItems(List<CartItemEntity> items) { this.items = items; }
    public User getUser()                            { return user; }
    public void setUser(User user)                   { this.user = user; }
}