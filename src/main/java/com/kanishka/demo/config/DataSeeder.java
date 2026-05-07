package com.kanishka.demo.config;

import com.kanishka.demo.catalog.Product;
import com.kanishka.demo.catalog.ProductRepository;
import com.kanishka.demo.user.Role;
import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedProductsIfEmpty();
        fixMissingProductImages(); // ✅ IMPORTANT: fixes old products too
    }

    private void seedAdminUser() {
        String adminEmail = "admin@golden.lk";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email(adminEmail)
                    .passwordHash(encoder.encode("Admin@123"))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .phone("071 857 9984")
                    .address("96B/2, Old Kesbewa Road, Nugegoda")
                    .build();

            userRepository.save(admin);
            System.out.println("✅ Admin user created: " + adminEmail);
        }
    }

    private void seedProductsIfEmpty() {
        if (productRepository.count() > 0) {
            System.out.println("✅ Products already seeded");
            return;
        }

        List<Product> products = new ArrayList<>();

        products.add(createProduct("GOLDEN", "Chilli Pieces", "1kg", "2000",
                "Premium quality chilli pieces for authentic Sri Lankan cuisine. Island-wide delivery available."));
        products.add(createProduct("GOLDEN", "Chilli Pieces", "250g", "1000",
                "Premium quality chilli pieces in convenient 250g pack. Perfect for home cooking."));
        products.add(createProduct("GOLDEN", "Chilli Powder", "1kg", "2000",
                "Finely ground chilli powder for adding heat to your dishes."));
        products.add(createProduct("GOLDEN", "Chilli Powder", "250g", "1000",
                "Finely ground chilli powder in 250g pack."));

        products.add(createProduct("GOLDEN", "Curry Powder", "1kg", "2000",
                "Authentic Sri Lankan curry powder blend with traditional spices."));
        products.add(createProduct("GOLDEN", "Curry Powder", "250g", "1000",
                "Authentic Sri Lankan curry powder in convenient 250g pack."));
        products.add(createProduct("GOLDEN", "Roasted Curry Powder", "1kg", "1000",
                "Dark roasted curry powder for rich, aromatic curries."));
        products.add(createProduct("GOLDEN", "Roasted Curry Powder", "250g", "500",
                "Dark roasted curry powder in 250g pack."));

        products.add(createProduct("GOLDEN", "Pepper Powder", "1kg", "2000",
                "Pure black pepper powder for enhanced flavor and aroma."));
        products.add(createProduct("GOLDEN", "Pepper Powder", "250g", "1000",
                "Pure black pepper powder in 250g pack."));
        products.add(createProduct("GOLDEN", "Turmeric Powder", "1kg", "1000",
                "Pure turmeric powder for color and health benefits."));
        products.add(createProduct("GOLDEN", "Turmeric Powder", "250g", "500",
                "Pure turmeric powder in convenient 250g pack."));

        products.add(createProduct("GOLDEN", "Meat Curry Mix", "1kg", "1000",
                "Special blend for preparing delicious meat curries."));
        products.add(createProduct("GOLDEN", "Meat Curry Mix", "250g", "500",
                "Special meat curry blend in 250g pack."));
        products.add(createProduct("GOLDEN", "Fish Curry Mix", "1kg", "1000",
                "Aromatic spice blend specially crafted for fish curries."));
        products.add(createProduct("GOLDEN", "Fish Curry Mix", "250g", "500",
                "Fish curry blend in convenient 250g pack."));

        productRepository.saveAll(products);
        System.out.println("✅ " + products.size() + " GOLDEN products seeded successfully");
    }

    // ✅ Automatically fill imageUrl for existing products (no need to delete DB)
    private void fixMissingProductImages() {
        List<Product> all = productRepository.findAll();
        boolean changed = false;

        for (Product p : all) {
            if (p.getImageUrl() == null || p.getImageUrl().trim().isEmpty()) {
                p.setImageUrl(imageFor(p.getName()));
                changed = true;
            }
        }

        if (changed) {
            productRepository.saveAll(all);
            System.out.println("✅ Product imageUrl fixed for existing records");
        }
    }

    private String imageFor(String name) {
        String n = name.toLowerCase();

        if (n.contains("chilli") && n.contains("pieces")) return "/images/products/chilli-pieces.png.jpg";
        if (n.contains("chilli") && n.contains("powder")) return "/images/products/chilli-powder.png.jpg";
        if (n.contains("roasted") && n.contains("curry")) return "/images/products/roasted-curry-powder.png.jpg";
        if (n.contains("meat") && n.contains("curry")) return "/images/products/meat-curry-mix.png.jpg";
        if (n.contains("pepper")) return "/images/products/pepper-powder.png.jpg";
        if (n.contains("curry")) return "/images/products/curry-powder.png.jpg";

        return "/images/products/curry-powder.png.jpg";
    }

    private Product createProduct(String brand, String name, String size, String price, String description) {
        return Product.builder()
                .brand(brand)
                .name(name)
                .sizeLabel(size)
                .priceLkr(new BigDecimal(price))
                .stockQty(100)
                .description(description)
                .imageUrl(imageFor(name)) // ✅ set imageUrl at creation
                .active(true)
                .build();
    }
}
