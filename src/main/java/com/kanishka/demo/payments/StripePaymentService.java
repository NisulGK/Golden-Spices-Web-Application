package com.kanishka.demo.payments;

import com.kanishka.demo.Order.Order;
import com.kanishka.demo.Order.OrderItem;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StripePaymentService {

    @Value("${stripe.secretKey:sk_test_YOUR_SECRET_KEY}")
    private String stripeSecretKey;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String createCheckoutSession(Order order) throws StripeException {
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(item.getProductName() + " - " + item.getProductSize())
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("lkr")
                            .setUnitAmount(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(100)).longValue())
                            .setProductData(productData)
                            .build();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setPriceData(priceData)
                            .setQuantity((long) item.getQuantity())
                            .build();

            lineItems.add(lineItem);
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/checkout/success?orderNumber=" + order.getOrderNumber())
                .setCancelUrl(baseUrl + "/checkout/cancel")
                .addAllLineItem(lineItems)
                .setCustomerEmail(order.getCustomerEmail())
                .putMetadata("orderNumber", order.getOrderNumber())
                .build();

        Session session = Session.create(params);

        // Save session ID to order
        order.setStripeSessionId(session.getId());

        return session.getUrl();
    }
}