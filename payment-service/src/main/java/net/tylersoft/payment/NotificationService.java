package net.tylersoft.payment;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.ReactiveHttpClient;
import net.tylersoft.payment.config.SmsProperties;
import net.tylersoft.payment.dto.SendSMSNotification;
import net.tylersoft.payment.service.OutgoingRequestLogService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SmsProperties smsProperties;
    private final ReactiveHttpClient httpClient;
    private final OutgoingRequestLogService logService;

    public void sendSmsNotification(String phoneNumber, String message) {
        String url = smsProperties.getApiUrl();
        String txId = String.valueOf(System.currentTimeMillis());
        var payload = new SendSMSNotification(
                phoneNumber, smsProperties.getSender(), phoneNumber,
                smsProperties.getUsername(), smsProperties.getPassword(),
                smsProperties.getClientId(), txId, message);

        logService.save(txId, "SMS", url, payload)
                .flatMap(savedLog -> httpClient.post(url, payload, String.class)
                        .flatMap(resp -> logService.updateSuccess(savedLog.getId(), "OK", resp).thenReturn(resp))
                        .onErrorResume(ex -> logService.updateFailure(savedLog.getId(), ex.getMessage())
                                .then(Mono.error(ex))))
                .subscribe(
                        resp -> System.out.println("SMS sent successfully: " + resp),
                        err -> System.err.println("Failed to send SMS: " + err.getMessage()));
    }
}
