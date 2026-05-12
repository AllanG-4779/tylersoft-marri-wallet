package net.tylersoft.wallet.admin;

import lombok.RequiredArgsConstructor;
import net.tylersoft.wallet.account.MiniStatementEntry;
import net.tylersoft.wallet.common.TransactionStatus;
import net.tylersoft.wallet.model.TrxMessage;
import net.tylersoft.wallet.repository.TrxMessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTransactionService {

    private static final int MAX_LIMIT = 200;

    private final TrxMessageRepository trxMessageRepository;

    public Flux<MiniStatementEntry> getTransactions(String accountNumber,
                                                    String from, String to,
                                                    String type, int limit) {
        int cap = Math.min(Math.max(limit, 1), MAX_LIMIT);
        OffsetDateTime start = parseDate(from, OffsetDateTime.now().minusMonths(3));
        OffsetDateTime end = parseDate(to, OffsetDateTime.now());

        Flux<TrxMessage> rows = type != null && !type.isBlank()
                ? trxMessageRepository.findAdminStatementByType(accountNumber, type.toUpperCase(), start, end, cap)
                : trxMessageRepository.findAdminStatement(accountNumber, start, end, cap);

        return rows.map(msg -> toEntry(msg, accountNumber));
    }

    public Mono<TransactionSummaryResponse> getSummary(String accountNumber, String from, String to) {
        OffsetDateTime start = parseDate(from, OffsetDateTime.now().minusMonths(3));
        OffsetDateTime end = parseDate(to, OffsetDateTime.now());

        return trxMessageRepository.findAdminStatement(accountNumber, start, end, MAX_LIMIT)
                .collectList()
                .map(messages -> buildSummary(messages, accountNumber));
    }

    private TransactionSummaryResponse buildSummary(List<TrxMessage> messages, String accountNumber) {
        Map<String, List<TrxMessage>> byType = messages.stream()
                .collect(Collectors.groupingBy(m -> m.getTransactionType() != null ? m.getTransactionType() : "UNKNOWN"));

        Map<String, Long> byStatus = messages.stream()
                .collect(Collectors.groupingBy(m -> resolveStatus(m.getStatus()), Collectors.counting()));

        List<TransactionSummaryResponse.TypeGroup> typeGroups = byType.entrySet().stream()
                .map(e -> new TransactionSummaryResponse.TypeGroup(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream()
                                .map(TrxMessage::getAmount)
                                .filter(a -> a != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .sorted(Comparator.comparing(TransactionSummaryResponse.TypeGroup::type))
                .collect(Collectors.toList());

        List<TransactionSummaryResponse.StatusGroup> statusGroups = byStatus.entrySet().stream()
                .map(e -> new TransactionSummaryResponse.StatusGroup(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(TransactionSummaryResponse.StatusGroup::status))
                .collect(Collectors.toList());

        BigDecimal totalVolume = messages.stream()
                .map(TrxMessage::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCharges = messages.stream()
                .map(TrxMessage::getTotalCharge)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionSummaryResponse(messages.size(), totalVolume, totalCharges, typeGroups, statusGroups);
    }

    private MiniStatementEntry toEntry(TrxMessage msg, String accountNumber) {
        String drCr = accountNumber.equals(msg.getDebitAccount()) ? "DR" : "CR";
        return new MiniStatementEntry(
                msg.getTransactionRef(),
                msg.getTransactionType(),
                msg.getTransactionCode(),
                drCr,
                msg.getAmount(),
                msg.getCurrency(),
                msg.getResponseMessage(),
                resolveStatus(msg.getStatus()),
                msg.getReceiptNumber(),
                msg.getCreatedOn()
        );
    }

    private String resolveStatus(Short code) {
        if (code == null) return "UNKNOWN";
        try {
            return TransactionStatus.fromCode(code).name();
        } catch (IllegalArgumentException e) {
            return "UNKNOWN";
        }
    }

    private OffsetDateTime parseDate(String date, OffsetDateTime fallback) {
        if (date == null || date.isBlank()) return fallback;
        try {
            return LocalDate.parse(date).atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
