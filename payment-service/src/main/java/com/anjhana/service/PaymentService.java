package com.anjhana.service;

import com.anjhana.model.Payment;
import com.anjhana.model.PaymentStatus;
import com.anjhana.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "inventory.reserved", groupId = "payment-service")
    @Transactional
    public void onInventoryReserved(Map<String, Object> event) {
        String orderId    = (String) event.get("orderId");
        String customerId = (String) event.get("customerId");
        // totalAmount comes as Double from JSON deserialization
        BigDecimal amount = new BigDecimal(event.get("totalAmount").toString());

        log.info("Processing payment for orderId: {} amount: {}", orderId, amount);

        Payment payment = Payment.builder()
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .status(PaymentStatus.PROCESSING)
            .build();
        paymentRepository.save(payment);

        try {
            String transactionId = chargePaymentGateway(customerId, amount);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(transactionId);
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            kafkaTemplate.send("payment.processed", orderId, Map.of(
                "orderId",       orderId,
                "transactionId", transactionId,
                "amount",        amount.toString(),
                "timestamp",     LocalDateTime.now().toString()
            ));
            log.info("Payment SUCCESS orderId: {} txId: {}", orderId, transactionId);

        } catch (Exception ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);

            kafkaTemplate.send("payment.failed", orderId, Map.of(
                "orderId",   orderId,
                "reason",    ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
            log.warn("Payment FAILED orderId: {} reason: {}", orderId, ex.getMessage());
        }
    }

    /**
     * Simulates payment gateway charge.
     * Circuit breaker wraps external gateway call.
     *
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentGatewayFallback")
    private String chargePaymentGateway(String customerId, BigDecimal amount) {
        // Simulate 10% failure rate for demo purposes
        if (Math.random() < 0.10) {
            throw new RuntimeException("Payment gateway timeout");
        }
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String paymentGatewayFallback(String customerId, BigDecimal amount, Exception ex) {
        throw new RuntimeException("Payment gateway unavailable: " + ex.getMessage());
    }

    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));
    }
}
