package net.tylersoft.users.client;

import java.util.List;

public record CustomerAccountsResponse(
        String status,
        String message,
        List<CustomerAccount> data,
        String error
) {}
