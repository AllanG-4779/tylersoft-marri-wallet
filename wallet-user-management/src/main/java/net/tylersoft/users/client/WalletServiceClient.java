package net.tylersoft.users.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class WalletServiceClient {

    private static final String ACCOUNTS_PATH = "/api/v2/accounts";

    private final WebClient webClient;

    public WalletServiceClient(
            @Value("${services.wallet-service.url}") String walletServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(walletServiceUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Calls the wallet-service to open a new wallet account for the customer.
     *
     * @param phoneNumber customer phone number — used as the account identifier
     * @param accountName customer's full name (used as the account display name)
     * @param currency    ISO currency code, e.g. {@code "KES"}
     * @return the newly created account number, or an error signal if creation failed
     */
    public Mono<String> createWalletAccount(String phoneNumber,
                                            String accountName,
                                            String currency) {
        CreateWalletAccountRequest body =
                CreateWalletAccountRequest.of(phoneNumber, accountName, currency);

        return webClient.post()
                .uri(ACCOUNTS_PATH)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(WalletAccountResponse.class)
                .flatMap(response -> {
                    if (response.success() && response.data() != null
                            && "00".equals(response.data().statusCode())) {
                        log.info("Wallet account created: phone={} accountNo={}",
                                phoneNumber, response.data().accountNo());
                        return Mono.just(response.data().accountNo());
                    }
                    String reason = response.data() != null
                            ? response.data().message()
                            : response.message();
                    return Mono.error(new RuntimeException(
                            "Wallet account creation failed: " + reason));
                })
                .doOnError(err -> log.error(
                        "Failed to create wallet account for phone={}: {}",
                        phoneNumber, err.getMessage()));
    }
}
