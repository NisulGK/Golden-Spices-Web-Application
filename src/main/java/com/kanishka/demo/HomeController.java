package com.kanishka.demo;

import com.kanishka.demo.catalog.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductRepository productRepository;

    @GetMapping("/")
    public String home(Model model) {
        // Pass active products to landing page for the featured products grid
        model.addAttribute("products", productRepository.findByActiveTrue());
        return "landing";
    }
}