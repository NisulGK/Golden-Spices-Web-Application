package com.kanishka.demo.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 80, message = "Name must be between 3 and 80 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 64, message = "Password must be between 6 and 64 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    // Phone: optional, but if provided must be digits only, 7–15 digits
    @Pattern(
            regexp = "^$|^[0-9]{7,15}$",
            message = "Phone number must contain digits only (7–15 digits)"
    )
    private String phone;

    private String address;
}