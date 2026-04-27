package net.tylersoft.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class SendSmsPayload {
    private String to;
    private String message;
    @Builder.Default
    private String from = "TenzTech";
    @Builder.Default
    private String transactionID = String.valueOf(System.currentTimeMillis());
    @JsonProperty("clientid")
    @Builder.Default
    private String clientId = "8";

    public SendSmsPayload(String to, String message) {
        this.to = to;
        this.message = message;
    }
}
