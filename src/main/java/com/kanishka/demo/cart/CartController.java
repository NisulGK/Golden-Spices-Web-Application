package com.kanishka.demo.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    // ── ADD ──────────────────────────────────────────────────
    @PostMapping("/add")
    public String add(@RequestParam Long productId,
                      @RequestParam(defaultValue = "1") int qty,
                      @AuthenticationPrincipal UserDetails principal,
                      RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Please log in to add items to your cart.");
            return "redirect:/auth/login";
        }
        try {
            cartService.add(principal.getUsername(), productId, qty);
            ra.addFlashAttribute("success", "Product added to cart!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    // ── UPDATE ────────────────────────────────────────────────
    @PostMapping("/update")
    public String update(@RequestParam Long productId,
                         @RequestParam int qty,
                         @AuthenticationPrincipal UserDetails principal,
                         RedirectAttributes ra) {

        if (principal == null) return "redirect:/auth/login";
        try {
            cartService.updateQuantity(principal.getUsername(), productId, qty);
            ra.addFlashAttribute("success", "Cart updated.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    // ── REMOVE ────────────────────────────────────────────────
    @PostMapping("/remove")
    public String remove(@RequestParam Long productId,
                         @AuthenticationPrincipal UserDetails principal,
                         RedirectAttributes ra) {

        if (principal == null) return "redirect:/auth/login";
        try {
            cartService.remove(principal.getUsername(), productId);
            ra.addFlashAttribute("success", "Item removed.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    // ── CLEAR ─────────────────────────────────────────────────
    @PostMapping("/clear")
    public String clear(@AuthenticationPrincipal UserDetails principal,
                        RedirectAttributes ra) {

        if (principal == null) return "redirect:/auth/login";
        cartService.clear(principal.getUsername());
        ra.addFlashAttribute("success", "Cart cleared.");
        return "redirect:/cart";
    }

    // ── VIEW ──────────────────────────────────────────────────
    @GetMapping
    public String view(@AuthenticationPrincipal UserDetails principal,
                       Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        Cart cart = cartService.getCartByUser(principal.getUsername());

        BigDecimal total = cart.getItems().stream()
                .map(CartItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = cart.getItems().stream()
                .mapToInt(CartItemEntity::getQuantity)
                .sum();

        // Validation flag — true if any item is out of stock or qty exceeds stock
        boolean hasInvalidItems = cart.getItems().stream()
                .anyMatch(i -> i.isOutOfStock() || i.exceedsStock());

        model.addAttribute("items",           cart.getItems());
        model.addAttribute("total",           total);
        model.addAttribute("itemCount",       itemCount);
        model.addAttribute("hasInvalidItems", hasInvalidItems);

        return "cart/view";
    }
}