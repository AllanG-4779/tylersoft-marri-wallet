package net.tylersoft.wallet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.model.ChargeConfig;
import net.tylersoft.wallet.model.ServiceManagement;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.ChargeConfigRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String SYSTEM_ACCOUNT_PREFIX = "SA";

    private final ServiceManagementRepository serviceManagementRepository;
    private final ChargeConfigRepository chargeConfigRepository;
    private final AccountRepository accountRepository;

    // ── Service Management ────────────────────────────────────────────────────

    public Mono<ServiceManagement> addServiceConfig(ServiceConfigRequest request) {
        if (request.isExternal() && request.accountId() == null) {
            return Mono.error(new IllegalArgumentException("accountId is required for external services"));
        }

        if (!request.isExternal()) {
            return saveServiceConfig(request);
        }

        return accountRepository.findById(request.accountId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Account not found: " + request.accountId())))
                .flatMap(account -> {
                    if (!account.getAccountNumber().startsWith(SYSTEM_ACCOUNT_PREFIX)) {
                        return Mono.error(new IllegalArgumentException(
                                "Account " + account.getAccountNumber()
                                + " is not a system account (must start with SA)"));
                    }
                    return saveServiceConfig(request);
                });
    }

    public Flux<ServiceManagement> listServiceConfigs() {
        return serviceManagementRepository.findAll();
    }

    public Mono<ServiceManagement> getServiceConfig(Integer id) {
        return serviceManagementRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Service config not found: " + id)));
    }

    public Mono<ServiceManagement> getServiceConfigByCode(String serviceCode) {
        return serviceManagementRepository.findByServiceCode(serviceCode)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Service config not found: " + serviceCode)));
    }

    private Mono<ServiceManagement> saveServiceConfig(ServiceConfigRequest request) {
        ServiceManagement svc = new ServiceManagement();
        svc.setServiceId(request.serviceId());
        svc.setExternalServiceId(request.externalServiceId());
        svc.setChannelId(request.channelId());
        svc.setServiceCode(request.serviceCode());
        svc.setExternal(request.isExternal());
        svc.setAccountId(request.accountId());
        svc.setSenderNarration(request.senderNarration());
        svc.setReceiverNarration(request.receiverNarration());
        svc.setDailyLimit(request.dailyLimit());
        svc.setWeeklyLimit(request.weeklyLimit());
        svc.setMonthlyLimit(request.monthlyLimit());
        svc.setDescription(request.description());
        svc.setStatus((short) 1);
        svc.setCreatedBy(request.createdBy());
        svc.setCreatedOn(OffsetDateTime.now());
        svc.setUpdatedOn(OffsetDateTime.now());
        log.info("Saving service config: serviceCode={} isExternal={}", request.serviceCode(), request.isExternal());
        return serviceManagementRepository.save(svc);
    }

    // ── Charge Config ─────────────────────────────────────────────────────────

    public Mono<ChargeConfig> addChargeConfig(ChargeConfigRequest request) {
        return serviceManagementRepository.existsById(request.serviceManagementId())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException(
                                "Service config not found: " + request.serviceManagementId()));
                    }
                    return saveChargeConfig(request);
                });
    }

    public Flux<ChargeConfig> listChargesByService(Integer serviceManagementId) {
        return chargeConfigRepository.findByServiceManagementId(serviceManagementId);
    }

    public Mono<ChargeConfig> getChargeConfig(Integer id) {
        return chargeConfigRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Charge config not found: " + id)));
    }

    private Mono<ChargeConfig> saveChargeConfig(ChargeConfigRequest request) {
        ChargeConfig charge = new ChargeConfig();
        charge.setServiceManagementId(request.serviceManagementId());
        charge.setMinAmount(request.minAmount());
        charge.setMaxAmount(request.maxAmount());
        charge.setChargeValue(request.chargeValue());
        charge.setValueType(request.valueType());
        charge.setChargeType(request.chargeType());
        charge.setAccountId(request.accountId());
        charge.setSenderNarration(request.senderNarration());
        charge.setReceiverNarration(request.receiverNarration());
        charge.setStatus((short) 1);
        charge.setCreatedBy(request.createdBy());
        charge.setCreatedOn(OffsetDateTime.now());
        charge.setUpdatedOn(OffsetDateTime.now());
        log.info("Saving charge config: serviceManagementId={} type={} valueType={}",
                request.serviceManagementId(), request.chargeType(), request.valueType());
        return chargeConfigRepository.save(charge);
    }
}
