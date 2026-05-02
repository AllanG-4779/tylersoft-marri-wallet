package net.tylersoft.wallet.ott;

record OttGatewayResponseData(String uniqueReference, String pin, String serialNumber, String rawResponse) {}

record OttGatewayResponse(String status, String message, OttGatewayResponseData data, String error) {}
