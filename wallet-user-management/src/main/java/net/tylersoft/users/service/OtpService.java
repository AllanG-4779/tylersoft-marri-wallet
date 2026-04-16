package net.tylersoft.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.users.model.OtpVerification;
import net.tylersoft.users.repository.OtpVerificationRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_TTL_MINUTES = 5;
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    private static final SecureRandom random = new SecureRandom();

    private final OtpVerificationRepository otpVerificationRepository;

    /**
     * Generates a 6-digit OTP, hashes it, persists an {@code otp_verifications} row,
     * and (stub) logs what would be sent via SMS.
     */
    public Mono<Void> generateAndSend(UUID customerId, String phoneNumber, String purpose) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        String hash = encoder.encode(otp);

        OtpVerification record = new OtpVerification();
        record.setId(UUID.randomUUID());
        record.setCustomerId(customerId);
        record.setPhoneNumber(phoneNumber);
        record.setOtpHash(hash);
        record.setPurpose(purpose);
        record.setUsed(false);
        record.setAttempts(0);
        record.setMaxAttempts(3);
        record.setExpiresAt(OffsetDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        record.setCreatedAt(OffsetDateTime.now());

        return otpVerificationRepository.save(record)
                .doOnSuccess(saved -> {
                    // TODO: forward to payment-service SMS endpoint
                    log.info("OTP for {} purpose={}: {}", phoneNumber, purpose, otp);
                })
                .then();
    }

    /**
     * Verifies the supplied OTP against the latest active record for this customer.
     * Increments attempts on mismatch; marks used on match.
     *
     * @return {@code true} if the OTP is correct, {@code false} otherwise.
     */
    public Mono<Boolean> verify(UUID customerId, String otp, String purpose) {
        return otpVerificationRepository.findLatestActive(customerId, purpose)
                .flatMap(record -> {
                    if (record.getAttempts() >= record.getMaxAttempts()) {
                        return Mono.error(new IllegalStateException(
                                "Maximum OTP attempts exceeded. Request a new OTP."));
                    }
                    if (encoder.matches(otp, record.getOtpHash())) {
                        return otpVerificationRepository.markUsed(record.getId())
                                .thenReturn(true);
                    }
                    return otpVerificationRepository.incrementAttempts(record.getId())
                            .thenReturn(false);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "No active OTP found. OTP may have expired — request a new one.")));
    }
}
