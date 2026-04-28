package net.tylersoft.wallet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.model.Account;
import net.tylersoft.wallet.model.ChargeConfig;
import net.tylersoft.wallet.model.ServiceManagement;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.AccountTypeRepository;
import net.tylersoft.wallet.repository.ChargeConfigRepository;
import net.tylersoft.wallet.repository.CurrencyRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import net.tylersoft.wallet.repository.SysServiceRepository;
import net.tylersoft.wallet.utils.CoreWalletUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String SYSTEM_ACCOUNT_PREFIX = "SA";
    private static final String DEFAULT_CURRENCY = "BWP";

    private final ServiceManagementRepository serviceManagementRepository;
    private final ChargeConfigRepository chargeConfigRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final SysServiceRepository sysServiceRepository;

    // ── Service Management ────────────────────────────────────────────────────

    public Mono<ServiceManagement> addServiceConfig(ServiceConfigRequest request, String authenticateduser) {

        return sysServiceRepository.findByTransactionType(request.transactionType())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Service not found in sys_services: " + request.serviceCode())))
                .flatMap(each -> serviceAlreadyConfigured(each.getId().longValue(), request.serviceCode()).then(Mono.just(each)))
                .flatMap(sysService -> createSystemAccount(request)
                        .flatMap(account -> saveServiceConfig(request, sysService.getId(), account.getId(), authenticateduser)));
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

    private Mono<Boolean> serviceAlreadyConfigured(Long serviceId, String serviceCode) {
        return serviceManagementRepository.existsByServiceCodeAndServiceId(serviceCode, serviceId.intValue())
                .flatMap(each -> {
                    if (each) {
                        return Mono.error(new IllegalArgumentException("Service already configured"));
                    }
                    return Mono.just(true);
                });
    }

    private Mono<Account> createSystemAccount(ServiceConfigRequest request) {
        String currency = request.currency() != null && !request.currency().isBlank()
                ? request.currency() : DEFAULT_CURRENCY;

        return accountTypeRepository.findByAccountPrefix(SYSTEM_ACCOUNT_PREFIX)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "System account type (prefix=" + SYSTEM_ACCOUNT_PREFIX + ") is not configured")))
                .flatMap(accountType ->
                        currencyRepository.findByCurrencyCode(currency)
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                        "Currency not found: " + currency)))
                                .flatMap(curr -> {
                                    String accountName = request.description() != null && !request.description().isBlank()
                                            ? request.description() : request.serviceCode() + " GL";
                                    Account account = new Account();
                                    account.setPhoneNumber(request.serviceCode());
                                    account.setCurrencyId(curr.getId());
                                    account.setAccountNumber("tmp_" + request.serviceCode());
                                    account.setAccountTypeId(accountType.getId());
                                    account.setAccountName(accountName);
                                    account.setAllowDr(true);
                                    account.setAllowCr(true);
                                    account.setBlocked(false);
                                    account.setDormant(false);
                                    account.setOpeningDate(OffsetDateTime.now());
                                    account.setOpeningBalance(BigDecimal.ZERO);
                                    account.setActualBalance(BigDecimal.ZERO);
                                    account.setAvailableBalance(BigDecimal.ZERO);
                                    account.setStatus((short) 1);
                                    account.setCreatedBy(request.createdBy());
                                    account.setCreatedOn(OffsetDateTime.now());
                                    account.setUpdatedOn(OffsetDateTime.now());
                                    return accountRepository.save(account)
                                            .flatMap(saved -> {
                                                String finalNumber = CoreWalletUtils.generate(
                                                        Math.toIntExact(saved.getId()),
                                                        SYSTEM_ACCOUNT_PREFIX,
                                                        accountType.getAccountNumberLength(),
                                                        true);
                                                saved.setAccountNumber(finalNumber);
                                                log.info("Created system account {} for service {}",
                                                        finalNumber, request.serviceCode());
                                                return accountRepository.save(saved);
                                            });
                                })
                );
    }

    private Mono<ServiceManagement> saveServiceConfig(ServiceConfigRequest request,
                                                      Integer sysServiceId,
                                                      Long accountId, String authenticatedUser) {
        ServiceManagement svc = new ServiceManagement();
        svc.setServiceId(sysServiceId);
        svc.setExternalServiceId(request.externalServiceId());
        svc.setServiceCode(request.serviceCode());
        svc.setExternal(request.isExternal());
        svc.setAccountId(accountId);
        svc.setSenderNarration(request.senderNarration());
        svc.setReceiverNarration(request.receiverNarration());
        svc.setDailyLimit(request.dailyLimit());
        svc.setWeeklyLimit(request.weeklyLimit());
        svc.setMonthlyLimit(request.monthlyLimit());
        svc.setDescription(request.description());
        svc.setStatus((short) 1);
        svc.setCreatedBy(authenticatedUser);
        svc.setCreatedOn(OffsetDateTime.now());
        svc.setUpdatedOn(OffsetDateTime.now());
        log.info("Saving service config: serviceCode={} sysServiceId={} isExternal={} accountId={}",
                request.serviceCode(), sysServiceId, request.isExternal(), accountId);
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
