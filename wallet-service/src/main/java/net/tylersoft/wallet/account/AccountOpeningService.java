package net.tylersoft.wallet.account;

import net.tylersoft.wallet.currency.CurrencyRepository;
import net.tylersoft.wallet.utils.CoreWalletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final AccountRepository     accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository    currencyRepository;

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
            String activeUser,
            String currency,
            String accountPrefix,
            String phoneNumber,
            String accountName,
            String reqType,
            BigDecimal openingBalance) {

        // -- Equivalent of: if strcmp(req_type, 'create') = 0 then ... else error end if
        if (!"create".equalsIgnoreCase(reqType)) {
            return Mono.just(OpenAccountResult.error("03", "Invalid account management request"));
        }

        // -- Equivalent of: select id, account_number_length into accountTypeId, accLength
        //                    from acc_account_types where account_prefix = accountPrefix
        //    with exit handler for not found → 01 / 'Invalid account prefix'
        return accountTypeRepository.findByAccountPrefix(accountPrefix)
                .switchIfEmpty(Mono.error(new OpenAccountException("01", "Invalid account prefix")))

                // -- Equivalent of: select id into currencyId from sys_currencies
                //                    where currency_code = currency limit 0,1
                //    with exit handler for not found → 14 / 'Invalid currency code'
                .flatMap(accountType ->
                        currencyRepository.findByCurrencyCode(currency)
                                .switchIfEmpty(Mono.error(new OpenAccountException("14", "Invalid currency code")))
                                .flatMap(curr -> {

                                    // -- Equivalent of:
                                    //    select concat('temp_', floor(rand(500)*400), '_', phoneNumber)
                                    //    into accountNumber;
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
                                    account.setOpeningBalance(openingBalance);
                                    account.setActualBalance(openingBalance);
                                    account.setAvailableBalance(openingBalance);
                                    account.setAccountTypeId(accountType.getId());
                                    account.setAccountName(accountName);
                                    account.setStatus((short) 1);
                                    account.setCreatedBy(activeUser);

                                    // -- insert into acc_accounts (...)
                                    return accountRepository.save(account)
                                            .flatMap(saved -> {

                                                // -- Equivalent of:
                                                //    call sp_generate_ac_number(accountId, accountPrefix,
                                                //                               accLength, true, accountNumber);
                                                String finalNumber = CoreWalletUtils.generate(
                                                        Math.toIntExact(saved.getId()),
                                                        accountPrefix,
                                                        accountType.getAccountNumberLength(),
                                                        false);

                                                // -- update acc_accounts set account_number=accountNumber
                                                //    where id = accountId
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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Equivalent of:
     * {@code select concat('temp_', floor(rand(500) * 400), '_', phoneNumber) into accountNumber}
     */
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
