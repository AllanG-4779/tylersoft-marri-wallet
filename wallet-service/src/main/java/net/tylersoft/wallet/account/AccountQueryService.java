package net.tylersoft.wallet.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.exception.exceptions.UnauthorizedException;
import net.tylersoft.wallet.common.TransactionStatus;
import net.tylersoft.wallet.config.CustomerOnly;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.CurrencyRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import net.tylersoft.wallet.repository.SysServiceRepository;
import net.tylersoft.wallet.repository.TrxMessageRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountQueryService {

    private static final int MAX_LIMIT = 50;

    private final AccountRepository         accountRepository;
    private final CurrencyRepository        currencyRepository;
    private final TrxMessageRepository      trxMessageRepository;
    private final SysServiceRepository      sysServiceRepository;
    private final ServiceManagementRepository serviceManagementRepository;

    @CustomerOnly
    public Mono<EnquiryResponse> enquire(Jwt jwt, AccountEnquiryRequest req) {
        String callerPhone = jwt.getClaimAsString("phone");
        String type        = req.transactionType();
        String code        = req.transactionCode();

        // 1. Validate enquiry type exists in sys_services and is marked as enquiry
        return sysServiceRepository.findByTransactionTypeAndIsEnquiryTrue(type)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Enquiry service not configured: " + type)))
                // 2. Validate transaction code exists in service config
                .flatMap(sysService ->
                        serviceManagementRepository.findByServiceIdAndServiceCode(sysService.getId(), code)
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                        "Enquiry type not configured: " + code))))
                // 3. Load account and enforce ownership
                .flatMap(svcConfig ->
                        accountRepository.findByAccountNumber(req.accountNumber())
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                        "Account not found: " + req.accountNumber())))
                                .flatMap(account -> {
                                    enforceOwnership(callerPhone, account.getPhoneNumber(), req.accountNumber());
                                    return resolveEnquiry(req, account.getCurrencyId());
                                })
                );
    }

    // ── Enquiry routing ────────────────────────────────────────────────────────

    private Mono<EnquiryResponse> resolveEnquiry(AccountEnquiryRequest req, Integer currencyId) {
        return switch (req.transactionCode().toUpperCase()) {
            case "BI_WALLET"       -> balanceEnquiry(req, currencyId);
            case "MINI_STATEMENT"  -> miniStatement(req);
            case "FULL_STATEMENT"  -> fullStatement(req);
            default -> Mono.error(new IllegalArgumentException(
                    "Unknown transaction code: " + req.transactionCode()));
        };
    }

    private Mono<EnquiryResponse> balanceEnquiry(AccountEnquiryRequest req, Integer currencyId) {
        return accountRepository.findByAccountNumber(req.accountNumber())
                .flatMap(account ->
                        currencyRepository.findById(currencyId)
                                .map(currency -> new BalanceResponse(
                                        account.getAccountNumber(),
                                        account.getAccountName(),
                                        currency.getIsoCode(),
                                        account.getActualBalance(),
                                        account.getAvailableBalance()
                                ))
                                .defaultIfEmpty(new BalanceResponse(
                                        account.getAccountNumber(),
                                        account.getAccountName(),
                                        "BWP",
                                        account.getActualBalance(),
                                        account.getAvailableBalance()
                                ))
                )
                .map(balance -> new EnquiryResponse(req.transactionType(), req.transactionCode(), balance, null));
    }

    private Mono<EnquiryResponse> miniStatement(AccountEnquiryRequest req) {
        int limit = Math.min(Math.max(req.limit() != null && req.limit() > 0 ? req.limit() : 10, 1), MAX_LIMIT);
        return trxMessageRepository.findMiniStatement(req.accountNumber(), limit)
                .map(msg -> toEntry(msg, req.accountNumber()))
                .collectList()
                .map(entries -> new EnquiryResponse(req.transactionType(), req.transactionCode(), null, entries));
    }

    private Mono<EnquiryResponse> fullStatement(AccountEnquiryRequest req) {
        OffsetDateTime from = parseDate(req.fromDate(), OffsetDateTime.now().minusMonths(1));
        OffsetDateTime to   = parseDate(req.toDate(),   OffsetDateTime.now());
        return trxMessageRepository.findStatement(req.accountNumber(), from, to)
                .map(msg -> toEntry(msg, req.accountNumber()))
                .collectList()
                .map(entries -> new EnquiryResponse(req.transactionType(), req.transactionCode(), null, entries));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MiniStatementEntry toEntry(net.tylersoft.wallet.model.TrxMessage msg, String accountNumber) {
        String drCr      = accountNumber.equals(msg.getDebitAccount()) ? "DR" : "CR";
        String statusName = resolveStatus(msg.getStatus());
        String narration  = buildNarration(msg.getTransactionType(), msg.getTransactionCode(), drCr);
        return new MiniStatementEntry(
                msg.getTransactionRef(),
                msg.getTransactionType(),
                msg.getTransactionCode(),
                drCr,
                msg.getAmount(),
                msg.getCurrency(),
                narration,
                statusName,
                msg.getReceiptNumber(),
                msg.getCreatedOn()
        );
    }

    private void enforceOwnership(String callerPhone, String accountPhone, String accountNumber) {
        if (callerPhone == null || !callerPhone.equals(accountPhone)) {
            log.warn("Ownership check failed: caller={} account={}", callerPhone, accountNumber);
            throw new UnauthorizedException("Access denied: account does not belong to this customer");
        }
    }

    private String resolveStatus(Short code) {
        if (code == null) return "UNKNOWN";
        try {
            return TransactionStatus.fromCode(code).name();
        } catch (IllegalArgumentException e) {
            return "UNKNOWN";
        }
    }

    private String buildNarration(String type, String code, String drCr) {
        String direction = "DR".equals(drCr) ? "Payment" : "Receipt";
        if (type == null) return direction;
        return switch (type.toUpperCase()) {
            case "DEPOSIT"    -> "CR".equals(drCr) ? "Top-up via " + nvl(code, "card") : "GL funding";
            case "WITHDRAWAL" -> "DR".equals(drCr) ? "Withdrawal via " + nvl(code, "channel") : "Withdrawal credit";
            case "FT"         -> "DR".equals(drCr) ? "Transfer sent" : "Transfer received";
            case "AIRTIME"    -> "Airtime purchase";
            case "BILL"       -> "Bill payment";
            default           -> direction + " - " + type;
        };
    }

    private OffsetDateTime parseDate(String date, OffsetDateTime fallback) {
        if (date == null || date.isBlank()) return fallback;
        try {
            return LocalDate.parse(date).atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value.toLowerCase() : fallback;
    }
}
