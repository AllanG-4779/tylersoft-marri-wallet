package net.tylersoft.users.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.http.ReactiveHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletServiceClient {

    private static final String ACCOUNTS_PATH = "/api/v2/accounts";

    private final ReactiveHttpClient httpClient;

    @Value("${services.wallet-service.url:http://localhost:8090}")
    private String walletServiceUrl;

    @Value("${services.wallet-service.username}")
    private String serviceUsername;

    @Value("${services.wallet-service.password}")
    private String servicePassword;

    public Mono<WalletAccountResponse> createWalletAccount(String phoneNumber,
                                                           String accountName,
                                                           String currency) {
        CreateWalletAccountRequest body = CreateWalletAccountRequest.of(phoneNumber, accountName, currency);
        return httpClient.post(
                walletServiceUrl + ACCOUNTS_PATH,
                Map.of("Authorization", basicAuth(serviceUsername, servicePassword)),
                body,
                WalletAccountResponse.class
        );
    }

    public Mono<List<CustomerAccount>> getAccountsByPhone(String phoneNumber) {
        return httpClient.get(
                walletServiceUrl + ACCOUNTS_PATH + "/by-phone/" + phoneNumber,
                Map.of("Authorization", basicAuth(serviceUsername, servicePassword)),
                CustomerAccountsResponse.class
        ).map(response -> response.data() != null ? response.data() : List.of());
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
