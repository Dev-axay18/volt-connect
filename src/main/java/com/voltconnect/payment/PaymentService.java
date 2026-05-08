package com.voltconnect.payment;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.voltconnect.shared.exceptions.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Service for Razorpay payment operations: order creation, signature verification,
 * and refund initiation.
 *
 * <p>Satisfies Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.8, 15.1.
 */
@Slf4j
@Service
public class PaymentService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    /**
     * Creates a Razorpay order for the given amount.
     *
     * @param amount    the booking total in INR
     * @param currency  the currency code (e.g. "INR")
     * @param bookingId the booking UUID used as the receipt reference
     * @return the Razorpay order ID string
     * @throws PaymentException if the Razorpay API call fails
     */
    public String createRazorpayOrder(BigDecimal amount, String currency, UUID bookingId) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);

            // Razorpay expects amount in smallest currency unit (paise for INR)
            int amountInPaise = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", bookingId.toString());

            Order order = client.orders.create(orderRequest);
            return order.get("id");

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for booking {}: {}", bookingId, e.getMessage());
            throw new PaymentException("Failed to create payment order: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies the Razorpay payment signature using HMAC-SHA256.
     *
     * <p>The expected signature is computed as:
     * {@code HMAC-SHA256(razorpaySecret, orderId + "|" + paymentId)}
     *
     * <p>Uses {@link MessageDigest#isEqual} for constant-time comparison to prevent
     * timing attacks.
     *
     * @param orderId   the Razorpay order ID
     * @param paymentId the Razorpay payment ID
     * @param signature the signature provided by the client
     * @return {@code true} if the signature is valid, {@code false} otherwise
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpaySecret.getBytes(), HMAC_SHA256);
            mac.init(secretKey);

            byte[] computedBytes = mac.doFinal(payload.getBytes());
            String computedSignature = bytesToHex(computedBytes);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    computedSignature.getBytes(),
                    signature.getBytes()
            );

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification failed due to crypto error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Initiates a refund for the given Razorpay payment.
     *
     * <p>Failures are logged but not re-thrown — refunds can be retried manually
     * via the Razorpay dashboard.
     *
     * @param razorpayPaymentId the payment ID to refund
     * @param amount            the amount to refund in INR
     */
    public void initiateRefund(String razorpayPaymentId, BigDecimal amount) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpaySecret);

            int amountInPaise = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInPaise);

            client.payments.refund(razorpayPaymentId, refundRequest);
            log.info("Refund initiated for payment {} amount {}", razorpayPaymentId, amount);

        } catch (RazorpayException e) {
            // Log but don't throw — refunds can be retried manually
            log.error("Failed to initiate refund for payment {}: {}", razorpayPaymentId, e.getMessage());
        }
    }

    /**
     * Returns the Razorpay publishable key ID (safe to send to clients).
     */
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
