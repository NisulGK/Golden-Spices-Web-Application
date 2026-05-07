package com.kanishka.demo.payments;

import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class MockPaymentValidator {

    /**
     * Demo/mock validation only.
     * Accepts any card details — no real payment gateway is used.
     * Only basic presence checks are performed so the form cannot be submitted empty.
     */
    public void validateCardPayment(String cardName,
                                    String cardNumber,
                                    String expiryMonth,
                                    String expiryYear,
                                    String cvv) {

        if (cardName == null || cardName.trim().isBlank()) {
            throw new IllegalArgumentException("Card holder name is required.");
        }

        String cleanCardNumber = sanitizeDigits(cardNumber);
        String cleanCvv = sanitizeDigits(cvv);
        String cleanMonth = sanitizeDigits(expiryMonth);
        String cleanYear = sanitizeDigits(expiryYear);

        if (cleanCardNumber.length() != 16) {
            throw new IllegalArgumentException("Card number must contain exactly 16 digits.");
        }

        // Luhn check intentionally skipped — this is a demo application.
        // Any 16-digit number is accepted.

        if (cleanCvv.length() != 3) {
            throw new IllegalArgumentException("CVV must contain exactly 3 digits.");
        }

        if (cleanMonth.isBlank() || cleanYear.isBlank()) {
            throw new IllegalArgumentException("Expiry month and year are required.");
        }

        int month;
        int year;

        try {
            month = Integer.parseInt(cleanMonth);
            year = Integer.parseInt(cleanYear);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Expiry date is invalid.");
        }

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Expiry month must be between 1 and 12.");
        }

        if (year < 100) {
            year = 2000 + year;
        }

        YearMonth now = YearMonth.now();
        YearMonth expiry = YearMonth.of(year, month);

        if (expiry.isBefore(now)) {
            throw new IllegalArgumentException("Card has expired.");
        }
    }

    private String sanitizeDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private boolean passesLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = cardNumber.charAt(i) - '0';

            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }

            sum += n;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}