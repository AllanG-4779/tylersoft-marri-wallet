package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.charge.ChargeType;
import net.tylersoft.wallet.model.ChargeConfig;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

public interface ChargeConfigRepository extends R2dbcRepository<ChargeConfig, Integer> {

    @Query("SELECT * FROM transaction_charges_config WHERE service_management_id = :serviceManagementId AND min_amount <= :amount AND max_amount >= :amount AND status = 1")
    Flux<ChargeConfig> findApplicable(Integer serviceManagementId, BigDecimal amount);

    Flux<ChargeConfig> findByServiceManagementId(Integer serviceManagementId);

    Flux<ChargeConfig> findByChargeType(ChargeType chargeType);
}
