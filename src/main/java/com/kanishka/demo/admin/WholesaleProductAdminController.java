package com.kanishka.demo.admin;

import com.kanishka.demo.wholesale.WholesaleProduct;
import com.kanishka.demo.wholesale.WholesaleProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/wholesale-products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WholesaleProductAdminController {

    private final WholesaleProductRepository repository;

    @PostMapping("/add")
    public String addProduct(@RequestParam String name,
                             @RequestParam BigDecimal price,
                             @RequestParam(defaultValue = "10") int minimumQuantity,
                             RedirectAttributes ra) {

        repository.save(
                WholesaleProduct.builder()
                        .name(name)
                        .price(price)
                        .minimumQuantity(minimumQuantity)
                        .active(true)
                        .build()
        );

        ra.addFlashAttribute("success", "Wholesale product '" + name + "' added successfully.");
        return "redirect:/admin/wholesale/dashboard";
    }

    @PostMapping("/{id}/toggle")
    public String toggleProduct(@PathVariable Long id, RedirectAttributes ra) {
        WholesaleProduct product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(!product.isActive());
        repository.save(product);
        ra.addFlashAttribute("success", "Product status updated.");
        return "redirect:/admin/wholesale/dashboard";
    }
}