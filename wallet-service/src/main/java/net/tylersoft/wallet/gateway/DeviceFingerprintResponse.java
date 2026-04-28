package net.tylersoft.wallet.gateway;

record DeviceFingerprintResponse(
        String statuscode,
        String tranid,
        String statusmessage,
        String id,
        String accessToken,
        String deviceDataCollectionUrl,
        String referenceId
) {}
