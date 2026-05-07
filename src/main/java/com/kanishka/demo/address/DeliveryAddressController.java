package com.kanishka.demo.address;

import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class DeliveryAddressController {

    private final DeliveryAddressService service;
    private final UserRepository         userRepository;

    private User getUser(UserDetails p) {
        return userRepository.findByEmail(p.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(@AuthenticationPrincipal UserDetails p, Model model) {
        model.addAttribute("addresses", service.getAddresses(getUser(p)));
        return "address/list";
    }

    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("req", new DeliveryAddressRequest());
        model.addAttribute("editMode", false);
        return "address/form";
    }

    @PostMapping("/new")
    public String save(@AuthenticationPrincipal UserDetails p,
                       @Valid @ModelAttribute("req") DeliveryAddressRequest req,
                       BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) { model.addAttribute("editMode", false); return "address/form"; }
        service.save(getUser(p), req);
        ra.addFlashAttribute("success", "Address saved successfully.");
        return "redirect:/addresses";
    }

    @GetMapping("/{id}/edit")
    @Transactional(readOnly = true)
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails p, Model model) {
        DeliveryAddress addr = service.getById(id);
        if (!addr.getUser().getId().equals(getUser(p).getId())) return "redirect:/addresses";
        DeliveryAddressRequest req = new DeliveryAddressRequest();
        req.setLabel(addr.getLabel()); req.setRecipientName(addr.getRecipientName());
        req.setPhone(addr.getPhone()); req.setAddressLine1(addr.getAddressLine1());
        req.setAddressLine2(addr.getAddressLine2()); req.setCity(addr.getCity());
        req.setDistrict(addr.getDistrict()); req.setPostalCode(addr.getPostalCode());
        req.setDefault(Boolean.TRUE.equals(addr.getIsDefault()));
        model.addAttribute("req", req); model.addAttribute("addressId", id); model.addAttribute("editMode", true);
        return "address/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @AuthenticationPrincipal UserDetails p,
                         @Valid @ModelAttribute("req") DeliveryAddressRequest req,
                         BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) { model.addAttribute("addressId", id); model.addAttribute("editMode", true); return "address/form"; }
        service.update(getUser(p), id, req);
        ra.addFlashAttribute("success", "Address updated.");
        return "redirect:/addresses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails p, RedirectAttributes ra) {
        service.delete(getUser(p), id);
        ra.addFlashAttribute("success", "Address deleted.");
        return "redirect:/addresses";
    }

    @PostMapping("/{id}/default")
    public String setDefault(@PathVariable Long id, @AuthenticationPrincipal UserDetails p, RedirectAttributes ra) {
        service.setDefault(getUser(p), id);
        ra.addFlashAttribute("success", "Default address updated.");
        return "redirect:/addresses";
    }
}