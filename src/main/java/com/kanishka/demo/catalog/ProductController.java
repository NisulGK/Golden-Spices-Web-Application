package com.kanishka.demo.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String search,
            Model model
    ) {
        if (search != null && !search.isBlank()) {
            model.addAttribute("products",
                    productRepository.findByNameContainingIgnoreCaseAndActiveTrue(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("products", productRepository.findByActiveTrue());
        }
        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        model.addAttribute("product", product);
        return "products/detail";
    }
}