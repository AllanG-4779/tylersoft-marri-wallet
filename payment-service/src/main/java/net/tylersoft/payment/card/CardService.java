package net.tylersoft.payment.card;

import lombok.RequiredArgsConstructor;
import net.tylersoft.payment.card.api.CardDeviceDataRequest;
import net.tylersoft.payment.card.api.CardPaymentRequest;
import net.tylersoft.payment.card.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CardService {
 // yyyy-MM-dd HH:mm:ss
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TcpClient tcpClient;
    private final TcpProperties props;

    public Mono<TcpDeviceDataResponse> deviceDataCollection(CardDeviceDataRequest req) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String currency = req.currency() != null ? req.currency() : props.getCurrency();
        String country = req.country() != null ? req.country() : props.getCountry();
        String authkey = buildAuthKey(req.tranid(), req.amount(), timestamp, req.phone());
        var tcpRequest = new TcpDeviceDataRequest(
                req.tranid(), country, req.amount(), authkey,
                req.firstname(), req.cardNumber(), props.getOrg(),
                req.cardExpiryYear(), props.getServiceId(), req.secondname(),
                req.cardCvv(), req.cardType(), props.getProcessingCode(),
                req.phone(), req.cardExpiryMonth(), currency, req.email(), timestamp
        );
        return tcpClient.deviceDataCollection(tcpRequest);
    }

    public Mono<TcpPaymentResponse> payment(CardPaymentRequest req) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String currency = req.currency() != null ? req.currency() : props.getCurrency();
        String country = req.country() != null ? req.country() : props.getCountry();
        String authkey = buildAuthKey(req.tranid(), req.amount(), timestamp, req.phone());
        var tcpRequest = new TcpPaymentRequest(
                req.tranid(), country, req.amount(), req.referenceId(), authkey,
                req.firstname(), req.cardNumber(), props.getOrg(),
                req.cardExpiryYear(), props.getServiceId(), req.secondname(),
                req.cardCvv(), req.cardType(), props.getProcessingCode(),
                req.phone(), req.cardExpiryMonth(), currency, req.email(), timestamp,
                req.ipAddress(), req.httpAcceptContent(), req.httpBrowserLanguage(),
                req.httpBrowserJavaEnabled(), req.httpBrowserJavaScriptEnabled(),
                req.httpBrowserColorDepth(), req.httpBrowserScreenHeight(),
                req.httpBrowserScreenWidth(), req.httpBrowserTimeDifference(),
                req.userAgentBrowserValue()
        );
        return tcpClient.payment(tcpRequest);
    }

    private String buildAuthKey(String tranid, String amount, String timestamp, String phone) {
        String hex;
        try {
            String raw = props.getKey() + tranid + amount + timestamp + phone;
            String base64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(base64.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte aByte : hash) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            hex = sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate authkey", e);
        }
        return hex;
    }

}
