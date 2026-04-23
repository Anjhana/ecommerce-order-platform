package com.anjhana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Notification Service — subscribes to all order lifecycle events
 * and sends email notifications to customers.
 *
 * Pure observer — reads from events, never publishes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @KafkaListener(topics = "order.created", groupId = "notification-service")
    public void onOrderCreated(Map<String, Object> event) {
        String orderId    = (String) event.get("orderId");
        String customerId = (String) event.get("customerId");
        log.info("[NOTIFICATION] Order created: {} for customer: {}", orderId, customerId);
        sendEmail(
            customerId + "@example.com",
            "Order Received — " + orderId,
            "Thank you! Your order " + orderId + " has been received and is being processed."
        );
    }

    @KafkaListener(topics = "order.confirmed", groupId = "notification-service")
    public void onOrderConfirmed(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        log.info("[NOTIFICATION] Order confirmed: {}", orderId);
        sendEmail(
            event.get("customerId") + "@example.com",
            "Order Confirmed — " + orderId,
            "Great news! Your order " + orderId + " has been confirmed and will be shipped soon."
        );
    }

    @KafkaListener(topics = "order.cancelled", groupId = "notification-service")
    public void onOrderCancelled(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String reason  = (String) event.get("reason");
        log.warn("[NOTIFICATION] Order cancelled: {} reason: {}", orderId, reason);
        sendEmail(
            event.get("customerId") + "@example.com",
            "Order Cancelled — " + orderId,
            "We're sorry. Your order " + orderId + " has been cancelled. Reason: " + reason +
            ". You will not be charged."
        );
    }

    @KafkaListener(topics = "payment.processed", groupId = "notification-service")
    public void onPaymentProcessed(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String txId    = (String) event.get("transactionId");
        log.info("[NOTIFICATION] Payment processed: {} txId: {}", orderId, txId);
        sendEmail(
            "customer@example.com",
            "Payment Confirmed — " + orderId,
            "Payment successful. Transaction ID: " + txId + ". Your order is confirmed!"
        );
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("orders@ecommerce.com");
            mailSender.send(message);
            log.info("Email sent to: {} subject: {}", to, subject);
        } catch (Exception e) {
            // Email failure must NOT fail the notification consumer
            // Log and continue — notifications are best-effort
            log.error("Failed to send email to: {} error: {}", to, e.getMessage());
        }
    }
}
