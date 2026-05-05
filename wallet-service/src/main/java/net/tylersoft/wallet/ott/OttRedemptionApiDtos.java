package net.tylersoft.wallet.ott;

// Package-private deserialization types matching payment-service ApiResponse<XyzResponse> shapes.

record WsCheckVoucherApiResponse(String status, String message, WsCheckVoucherData data, String error) {}
record WsCheckVoucherData(boolean success, String serial, String voucherId, String value, String message, String errorCode) {}

record WsRemitApiResponse(String status, String message, WsRemitData data, String error) {}
record WsRemitData(boolean success, String voucherId, String voucherAmount, String voucherBalance, String errorCode, String message) {}

record WsCheckRemitApiResponse(String status, String message, WsCheckRemitData data, String error) {}
record WsCheckRemitData(boolean success, String voucherId, String voucherAmount, String voucherBalance, String serialNumber, String errorCode, String message) {}
