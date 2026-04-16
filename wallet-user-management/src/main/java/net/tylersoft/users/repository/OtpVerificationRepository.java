package net.tylersoft.users.repository;

import net.tylersoft.users.model.OtpVerification;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OtpVerificationRepository extends R2dbcRepository<OtpVerification, UUID> {

    @Query("""
            SELECT * FROM users.otp_verifications
            WHERE customer_id = :customerId
              AND purpose = :purpose
              AND is_used = FALSE
              AND expires_at > NOW()
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Mono<OtpVerification> findLatestActive(UUID customerId, String purpose);

    @Modifying
    @Query("""
            UPDATE users.otp_verifications
            SET is_used = TRUE, used_at = NOW(), attempts = attempts + 1
            WHERE id = :id
            """)
    Mono<Integer> markUsed(UUID id);

    @Modifying
    @Query("""
            UPDATE users.otp_verifications
            SET attempts = attempts + 1
            WHERE id = :id
            """)
    Mono<Integer> incrementAttempts(UUID id);
}
