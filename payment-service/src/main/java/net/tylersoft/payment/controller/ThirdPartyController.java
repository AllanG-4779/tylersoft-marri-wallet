package net.tylersoft.payment.controller;

import net.tylersoft.payment.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/third-party")
public class ThirdPartyController {

    private final NotificationService notificationService;

    public ThirdPartyController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/send-sms/{phoneNumber}/{message}")
    public void sendSms(@PathVariable String message, @PathVariable String phoneNumber) {
        notificationService.sendSmsNotification(phoneNumber, message);
    }

}
