package net.tylersoft.payment.card.dto;

public record TcpDeviceDataResponse(
        String statuscode,
        String tranid,
        String statusmessage,
        String id,
        String accessToken,
        String deviceDataCollectionUrl,
        String referenceId
) {}
