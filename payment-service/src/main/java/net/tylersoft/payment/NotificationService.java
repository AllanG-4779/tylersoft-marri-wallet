package net.tylersoft.payment;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.ReactiveHttpClient;
import net.tylersoft.payment.config.SmsProperties;
import net.tylersoft.payment.dto.SendSMSNotification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class NotificationService {
    private final SmsProperties smsProperties;
    private final ReactiveHttpClient httpClient;

    public void sendSmsNotification(String phoneNumber, String message) {
        String url = smsProperties.getApiUrl();
        var payload = new SendSMSNotification(phoneNumber, smsProperties.getSender(), phoneNumber,
                smsProperties.getUsername(), smsProperties.getPassword(), smsProperties.getClientId(), String.valueOf(System.currentTimeMillis()), message);

        // Send the SMS notification
        httpClient.post(url, payload, String.class)
                .subscribe(response -> {
                    // Handle the response if needed
                    System.out.println("SMS sent successfully: " + response);
                }, error -> {
                    // Handle any errors that occur during the request
                    System.err.println("Failed to send SMS: " + error.getMessage());
                });
    }


}
