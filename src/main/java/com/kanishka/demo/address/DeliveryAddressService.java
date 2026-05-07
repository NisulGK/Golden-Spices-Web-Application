package com.kanishka.demo.address;

import com.kanishka.demo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryAddressService {

    private final DeliveryAddressRepository repo;

    public List<DeliveryAddress> getAddresses(User user) {
        return repo.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
    }

    public DeliveryAddress getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Address not found"));
    }

    @Transactional
    public DeliveryAddress save(User user, DeliveryAddressRequest req) {
        long count = repo.countByUser(user);
        boolean makeDefault = req.isDefault() || count == 0;
        if (makeDefault) repo.clearDefaultForUser(user);
        DeliveryAddress addr = DeliveryAddress.builder()
                .user(user).label(req.getLabel())
                .recipientName(req.getRecipientName()).phone(req.getPhone())
                .addressLine1(req.getAddressLine1()).addressLine2(req.getAddressLine2())
                .city(req.getCity()).district(req.getDistrict())
                .postalCode(req.getPostalCode()).isDefault(makeDefault).build();
        return repo.save(addr);
    }

    @Transactional
    public DeliveryAddress update(User user, Long id, DeliveryAddressRequest req) {
        DeliveryAddress addr = getById(id);
        if (!addr.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        if (req.isDefault()) repo.clearDefaultForUser(user);
        addr.setLabel(req.getLabel()); addr.setRecipientName(req.getRecipientName());
        addr.setPhone(req.getPhone()); addr.setAddressLine1(req.getAddressLine1());
        addr.setAddressLine2(req.getAddressLine2()); addr.setCity(req.getCity());
        addr.setDistrict(req.getDistrict()); addr.setPostalCode(req.getPostalCode());
        addr.setIsDefault(req.isDefault());
        return repo.save(addr);
    }

    @Transactional
    public void delete(User user, Long id) {
        DeliveryAddress addr = getById(id);
        if (!addr.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        boolean wasDefault = Boolean.TRUE.equals(addr.getIsDefault());
        repo.delete(addr);
        if (wasDefault) {
            repo.findByUserOrderByIsDefaultDescCreatedAtDesc(user)
                    .stream().findFirst()
                    .ifPresent(a -> { a.setIsDefault(true); repo.save(a); });
        }
    }

    @Transactional
    public void setDefault(User user, Long id) {
        DeliveryAddress addr = getById(id);
        if (!addr.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");
        repo.clearDefaultForUser(user);
        addr.setIsDefault(true);
        repo.save(addr);
    }
}