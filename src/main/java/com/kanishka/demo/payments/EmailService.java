package com.kanishka.demo.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendInvoiceEmail(String toEmail,
                                 String customerName,
                                 String orderNumber,
                                 byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your GOLDEN Receipt - " + orderNumber);

            String safeName = customerName == null || customerName.isBlank() ? "Customer" : customerName;

            String html = """
                    <html>
                    <body style="font-family: Arial, sans-serif; color: #333;">
                        <h2 style="color:#c8841f;">Thank you for your order</h2>
                        <p>Hi %s,</p>
                        <p>Your payment was completed successfully.</p>
                        <p><strong>Order Number:</strong> %s</p>
                        <p>Your PDF receipt is attached to this email.</p>
                        <br>
                        <p>Regards,<br><strong>GOLDEN Dissanayaka Distributors</strong></p>
                    </body>
                    </html>
                    """.formatted(safeName, orderNumber);

            helper.setText(html, true);
            helper.addAttachment("Receipt-" + orderNumber + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send invoice email", e);
        }
    }

    public void sendWholesaleOrderConfirmation(String toEmail,
                                               String customerName,
                                               Long orderId,
                                               String productName,
                                               int quantity,
                                               String totalAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Order Confirmation #" + orderId + " - GOLDEN Dissanayaka Distributors");

            String safeName = customerName == null || customerName.isBlank() ? "Valued Customer" : customerName;

            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <style>\n" +
                    "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                    "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); padding: 20px; }\n" +
                    "        .wrapper { max-width: 650px; margin: 0 auto; }\n" +
                    "        .email-container { background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12); }\n" +
                    "        .header { background: linear-gradient(135deg, #c8841f 0%, #8a5c0c 100%); padding: 40px 30px; text-align: center; }\n" +
                    "        .header h1 { color: #ffffff; font-size: 28px; font-weight: 600; letter-spacing: 0.5px; margin-bottom: 8px; }\n" +
                    "        .header p { color: rgba(255, 255, 255, 0.9); font-size: 14px; }\n" +
                    "        .content { padding: 40px 30px; }\n" +
                    "        .greeting { color: #333; font-size: 16px; margin-bottom: 20px; line-height: 1.6; }\n" +
                    "        .greeting strong { color: #c8841f; font-weight: 600; }\n" +
                    "        .intro-text { color: #666; font-size: 15px; line-height: 1.7; margin-bottom: 30px; }\n" +
                    "        .order-box { background: linear-gradient(135deg, #fdf4e3 0%, #f5e6c0 100%); border-left: 4px solid #c8841f; padding: 25px; border-radius: 8px; margin: 30px 0; }\n" +
                    "        .order-box h3 { color: #8a5c0c; font-size: 13px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 15px; font-weight: 600; }\n" +
                    "        .order-detail { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid rgba(200, 132, 31, 0.2); }\n" +
                    "        .order-detail:last-child { border-bottom: none; }\n" +
                    "        .order-label { color: #666; font-size: 14px; font-weight: 500; }\n" +
                    "        .order-value { color: #333; font-size: 15px; font-weight: 600; }\n" +
                    "        .order-value.highlight { color: #c8841f; font-size: 18px; font-weight: 700; }\n" +
                    "        .next-steps { background: #f9f9f9; padding: 25px; border-radius: 8px; margin: 30px 0; }\n" +
                    "        .next-steps h3 { color: #333; font-size: 16px; font-weight: 600; margin-bottom: 15px; }\n" +
                    "        .next-steps p { color: #666; font-size: 14px; line-height: 1.8; margin-bottom: 12px; }\n" +
                    "        .next-steps p:last-child { margin-bottom: 0; }\n" +
                    "        .cta-button { display: inline-block; background: linear-gradient(135deg, #c8841f 0%, #8a5c0c 100%); color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600; margin: 20px 0; transition: transform 0.3s ease, box-shadow 0.3s ease; box-shadow: 0 4px 12px rgba(200, 132, 31, 0.3); }\n" +
                    "        .cta-button:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(200, 132, 31, 0.4); }\n" +
                    "        .contact-info { background: #f9f9f9; padding: 25px; border-radius: 8px; margin: 30px 0; text-align: center; }\n" +
                    "        .contact-info h4 { color: #333; font-size: 14px; font-weight: 600; margin-bottom: 10px; }\n" +
                    "        .contact-info p { color: #666; font-size: 13px; margin: 5px 0; }\n" +
                    "        .footer { background: #1a1208; color: #999; padding: 25px 30px; text-align: center; font-size: 12px; line-height: 1.8; }\n" +
                    "        .footer p { margin: 5px 0; }\n" +
                    "        .divider { height: 1px; background: #ddd; margin: 0; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"wrapper\">\n" +
                    "        <div class=\"email-container\">\n" +
                    "            <div class=\"header\">\n" +
                    "                <h1>Order Confirmed</h1>\n" +
                    "                <p>Your wholesale order has been successfully received</p>\n" +
                    "            </div>\n" +
                    "            <div class=\"content\">\n" +
                    "                <p class=\"greeting\">Dear <strong>" + safeName + "</strong>,</p>\n" +
                    "                <p class=\"intro-text\">Thank you for placing your wholesale order with GOLDEN Dissanayaka Distributors. We appreciate your business and look forward to serving you.</p>\n" +
                    "                <div class=\"order-box\">\n" +
                    "                    <h3>Order Details</h3>\n" +
                    "                    <div class=\"order-detail\">\n" +
                    "                        <span class=\"order-label\">Order Number</span>\n" +
                    "                        <span class=\"order-value\">#" + orderId + "</span>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"order-detail\">\n" +
                    "                        <span class=\"order-label\">Product</span>\n" +
                    "                        <span class=\"order-value\">" + productName + "</span>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"order-detail\">\n" +
                    "                        <span class=\"order-label\">Quantity</span>\n" +
                    "                        <span class=\"order-value\">" + quantity + " units</span>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"order-detail\">\n" +
                    "                        <span class=\"order-label\">Total Amount</span>\n" +
                    "                        <span class=\"order-value highlight\">LKR " + totalAmount + "</span>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "                <div class=\"next-steps\">\n" +
                    "                    <h3>What Happens Next</h3>\n" +
                    "                    <p>Our team will review your order promptly and process it for shipment. You will receive updates on your order status via email.</p>\n" +
                    "                    <p>Track your order anytime by logging into your wholesale dashboard:</p>\n" +
                    "                </div>\n" +
                    "                <center>\n" +
                    "                    <a href=\"http://localhost:8080/wholesale\" class=\"cta-button\">View Order Dashboard</a>\n" +
                    "                </center>\n" +
                    "                <div class=\"contact-info\">\n" +
                    "                    <h4>Need Assistance?</h4>\n" +
                    "                    <p>If you have any questions about your order, please contact our wholesale team</p>\n" +
                    "                    <p>Email: orders@golden.lk</p>\n" +
                    "                </div>\n" +
                    "                <p style=\"color: #666; font-size: 14px; margin-top: 30px; text-align: center;\">Best regards,<br><strong style=\"color: #c8841f; font-size: 15px;\">GOLDEN Dissanayaka Distributors</strong><br>Wholesale Department</p>\n" +
                    "            </div>\n" +
                    "            <div class=\"divider\"></div>\n" +
                    "            <div class=\"footer\">\n" +
                    "                <p>This is an automated confirmation email. Please do not reply directly to this email.</p>\n" +
                    "                <p>96B/2, Old Kesbewa Road, Nugegoda | Tel: 071 857 9984</p>\n" +
                    "                <p>Copyright GOLDEN Dissanayaka Distributors. All rights reserved.</p>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send wholesale order confirmation email to " + toEmail);
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send wholesale order confirmation email: " + e.getMessage(), e);
        }
    }
}