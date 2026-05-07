package com.kanishka.demo.address;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DeliveryAddressRequest {

    @NotBlank(message = "Label is required (e.g. Home, Office)")
    @Size(max = 40)
    private String label;

    @NotBlank(message = "Recipient name is required")
    @Size(min = 2, max = 80)
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "Phone must be digits only (7-15 digits)")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 80)
    private String city;

    @Size(max = 80)
    private String district;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9]{5}$", message = "Postal code must be 5 digits")
    private String postalCode;

    private boolean isDefault;

    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public boolean isDefault() { return isDefault; }
}