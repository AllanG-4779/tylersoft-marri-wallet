package net.tylersoft.wallet.admin;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.wallet.account.MiniStatementEntry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    @GetMapping
    public Mono<ApiResponse<List<MiniStatementEntry>>> list(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "50") int limit) {
        return adminTransactionService
                .getTransactions(accountNumber, from, to, type, limit)
                .collectList()
                .map(ApiResponse::ok);
    }

    @GetMapping("/summary")
    public Mono<ApiResponse<TransactionSummaryResponse>> summary(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminTransactionService
                .getSummary(accountNumber, from, to)
                .map(ApiResponse::ok);
    }
}
