package net.tylersoft.users.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.http.ReactiveHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletServiceClient {

    private static final String ACCOUNTS_PATH = "/api/v2/accounts";

    private final ReactiveHttpClient httpClient;
    /**
     * Calls the wallet-service to open a new wallet account for the customer.
     *
     * @param phoneNumber customer phone number — used as the account identifier
     * @param accountName customer's full name (used as the account display name)
     * @param currency    ISO currency code, e.g. {@code "KES"}
     * @return the newly created account number, or an error signal if creation failed
     */
    public Mono<WalletAccountResponse> createWalletAccount(String phoneNumber,
                                            String accountName,
                                            String currency) {
        CreateWalletAccountRequest body =
                CreateWalletAccountRequest.of(phoneNumber, accountName, currency);

        return httpClient.post(String.format("%s%s", "http://localhost:8090", ACCOUNTS_PATH), body, WalletAccountResponse.class);

    }
}
