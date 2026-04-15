package net.tylersoft.wallet.account;

import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.AccountTypeRepository;
import net.tylersoft.wallet.repository.CurrencyRepository;
import net.tylersoft.wallet.model.Account;
import net.tylersoft.wallet.utils.CoreWalletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Reactive Java equivalent of the {@code sp_open_account} stored procedure.
 *
 * <p>Mirrors the procedure's error codes:
 * <ul>
 *   <li>{@code 00} – success
 *   <li>{@code 01} – invalid account prefix
 *   <li>{@code 03} – invalid req_type (only {@code "create"} is accepted)
 *   <li>{@code 14} – invalid currency code
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountOpeningService {

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;

    /**
     * Open a new wallet account.
     *
     * @param activeUser     authenticated user performing the action (created_by)
     * @param currency       ISO currency code, e.g. "KES"
     * @param accountPrefix  account-type prefix, e.g. "TA"
     * @param phoneNumber    owner's phone number
     * @param accountName    display name for the account
     * @param reqType        must be {@code "create"} (mirrors the IN param of the SP)
     * @param openingBalance initial balance credited to the account
     * @return {@link OpenAccountResult} carrying status_code, message, and accountNo
     */
    @Transactional
    public Mono<OpenAccountResult> openAccount(
            String currency,
            String accountPrefix,
            String phoneNumber,
            String accountName,
            String reqType, @ModelAttribute String authenticatedUser) {

        // -- Equivalent of: if strcmp(req_type, 'create') = 0 then ... else error end if
        if (!"create".equalsIgnoreCase(reqType)) {
            return Mono.just(OpenAccountResult.error("03", "Invalid account management request"));
        }
        return accountTypeRepository.findByAccountPrefix(accountPrefix)
                .switchIfEmpty(Mono.error(new OpenAccountException("01", "Invalid account prefix")))
                .flatMap(each -> {
                    log.info("Checking accountType for prefix {}: found {}", accountPrefix, each.getId());
                    return accountRepository.countAllByPhoneNumberAndAccountTypeId(phoneNumber, each.getId())
                            .flatMap(count -> {
                                if (count > each.getMaxAccounts()) {
                                    return Mono.error(new OpenAccountException("01", "Account limit reached for this account type"));
                                }
                                return Mono.just(each);
                            });
                })
                .flatMap(accountType ->
                        currencyRepository.findByCurrencyCode(currency)
                                .switchIfEmpty(Mono.error(new OpenAccountException("14", "Invalid currency code")))
                                .flatMap(curr -> {
                                    String tempNumber = buildTempAccountNumber(phoneNumber);
                                    Account account = new Account();
                                    account.setPhoneNumber(phoneNumber);
                                    account.setCurrencyId(curr.getId());
                                    account.setAccountNumber(tempNumber);
                                    account.setAllowDr(true);
                                    account.setAllowCr(true);
                                    account.setBlocked(false);
                                    account.setDormant(false);
                                    account.setOpeningDate(OffsetDateTime.now());
                                    account.setOpeningBalance(BigDecimal.valueOf(0.00));
                                    account.setActualBalance(BigDecimal.valueOf(0.00));
                                    account.setAvailableBalance(BigDecimal.valueOf(0.00));
                                    account.setAccountTypeId(accountType.getId());
                                    account.setAccountName(accountName);
                                    account.setStatus((short) 1);
                                    account.setCreatedBy(authenticatedUser);
                                    return accountRepository.save(account)
                                            .flatMap(saved -> {
                                                String finalNumber = CoreWalletUtils.generate(
                                                        Math.toIntExact(saved.getId()),
                                                        accountPrefix,
                                                        accountType.getAccountNumberLength(),
                                                        true);
                                                saved.setAccountNumber(finalNumber);
                                                return accountRepository.save(saved)
                                                        .map(updated -> {
                                                            log.info("Account opened: {} for phone {}",
                                                                    updated.getAccountNumber(), phoneNumber);
                                                            return OpenAccountResult.success(updated.getAccountNumber());
                                                        });
                                            });
                                })
                )
                // -- Mirror the exit handlers: translate domain exceptions into result codes
                .onErrorResume(OpenAccountException.class, ex ->
                        Mono.just(OpenAccountResult.error(ex.statusCode, ex.getMessage())))
                .onErrorResume(ex -> {
                    log.error("Unexpected error during account opening for phone={}", phoneNumber, ex);
                    return Mono.just(OpenAccountResult.error("99", "Unexpected error: " + ex.getMessage()));
                });
    }


    private static String buildTempAccountNumber(String phoneNumber) {
        long rand = (long) (Math.random() * 500 * 400);
        return "temp_" + rand + "_" + phoneNumber;
    }

    // -------------------------------------------------------------------------
    // Private exception — carries the SP's status_code alongside the message
    // -------------------------------------------------------------------------

    private static final class OpenAccountException extends RuntimeException {
        final String statusCode;

        OpenAccountException(String statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
