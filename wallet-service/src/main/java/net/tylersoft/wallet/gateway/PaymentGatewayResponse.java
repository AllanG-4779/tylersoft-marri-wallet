package net.tylersoft.wallet.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

record PaymentGatewayResponse(
        String pareq,
        String pastepup,
        String accessToken,
        String message,
    String statuscode
) {}
