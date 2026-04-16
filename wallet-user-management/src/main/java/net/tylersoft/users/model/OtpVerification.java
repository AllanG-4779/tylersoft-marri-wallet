package net.tylersoft.users.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "users", value = "otp_verifications")
@Getter @Setter @NoArgsConstructor
public class OtpVerification {

    @Id
    private UUID id;

    private UUID customerId;
    private String phoneNumber;
    private String otpHash;
    private String purpose;
    private boolean isUsed;
    private int attempts;
    private int maxAttempts;
    private OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;
    private OffsetDateTime createdAt;
}
