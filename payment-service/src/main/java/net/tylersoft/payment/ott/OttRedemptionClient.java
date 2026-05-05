package net.tylersoft.payment.ott;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.payment.ott.dto.*;
import net.tylersoft.payment.service.OutgoingRequestLogService;
import net.tylersoft.payment.utils.GeneralUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP client for the OTT Mobile Redemption API (v6.1).
 * <p>
 * Auth: Basic (username:password from OttProperties).
 * Hash: SHA-256(apiKey + form-values in alphabetical param-name order).
 * Content-Type: application/x-www-form-urlencoded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OttRedemptionClient {

    private final OttProperties          props;
    private final WebClient              webClient;
    private final OutgoingRequestLogService logService;
    private final ObjectMapper           objectMapper;

    // ── CheckVoucher ──────────────────────────────────────────────────────────

    public Mono<OttCheckVoucherResponse> checkVoucher(OttCheckVoucherRequest request) {
        String url = props.getBaseUrl() + props.getCheckVoucherEndpoint();

        // Hash params (alphabetical): vendorID < voucherPIN
        Map<String, Object> hashFields = new LinkedHashMap<>();
        hashFields.put("vendorID",    String.valueOf(props.getVendorId()));
        hashFields.put("voucherPIN",  request.voucherPin());
        String hash = GeneralUtils.generateHashedKey(hashFields, props.getApiKey());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("vendorID",    String.valueOf(props.getVendorId()));
        form.add("voucherPIN",  request.voucherPin());
        form.add("Hash", hash);   // spec uses capital H for CheckVoucher

        String logRef = "CV-" + mask(request.voucherPin());

        return logService.save(logRef, "OTT_CHECK_VOUCHER", url, form.toSingleValueMap())
                .flatMap(savedLog ->
                        post(url, form)
                                .doOnSuccess(raw -> log.info("OTT CheckVoucher ref={}: {}", logRef, raw))
                                .flatMap(raw -> logService.updateSuccess(savedLog.getId(), "OK", raw)
                                        .thenReturn(parseCheckVoucher(raw)))
                                .onErrorResume(ex -> logService.updateFailure(savedLog.getId(), ex.getMessage())
                                        .then(Mono.error(ex))));
    }

    // ── RemitVoucher ──────────────────────────────────────────────────────────

    public Mono<OttRemitVoucherResponse> remitVoucher(OttRemitVoucherRequest request) {
        String url = props.getBaseUrl() + props.getRemitVoucherEndpoint();

        // Hash all present params in alphabetical order: account, amount, clientID, mobile, pin, uniqueReference, vendorID
        Map<String, Object> hashFields = new LinkedHashMap<>();
        if (request.account() != null)   hashFields.put("account",          request.account());
        hashFields.put("amount",          request.amount());
        if (request.clientId() != null)  hashFields.put("clientID",         request.clientId());
        hashFields.put("mobile",          request.mobile());
        hashFields.put("pin",             request.pin());
        hashFields.put("uniqueReference", request.uniqueReference());
        hashFields.put("vendorID",        String.valueOf(props.getVendorId()));
        String hash = GeneralUtils.generateHashedKey(hashFields, props.getApiKey());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (request.account() != null)  form.add("account",          request.account());
        form.add("amount",              request.amount());
        if (request.clientId() != null) form.add("clientID",         request.clientId());
        form.add("mobile",              request.mobile());
        form.add("pin",                 request.pin());
        form.add("uniqueReference",     request.uniqueReference());
        form.add("vendorID",            String.valueOf(props.getVendorId()));
        form.add("hash",                hash);

        return logService.save(request.uniqueReference(), "OTT_REMIT_VOUCHER", url, form.toSingleValueMap())
                .flatMap(savedLog ->
                        post(url, form)
                                .doOnSuccess(raw -> log.info("OTT RemitVoucher ref={}: {}", request.uniqueReference(), raw))
                                .flatMap(raw -> logService.updateSuccess(savedLog.getId(), "OK", raw)
                                        .thenReturn(parseRemitVoucher(raw)))
                                .onErrorResume(ex -> logService.updateFailure(savedLog.getId(), ex.getMessage())
                                        .then(Mono.error(ex))));
    }

    // ── CheckRemitVoucher ─────────────────────────────────────────────────────

    public Mono<OttCheckRemitVoucherResponse> checkRemitVoucher(OttCheckRemitVoucherRequest request) {
        String url = props.getBaseUrl() + props.getCheckRemitVoucherEndpoint();

        // Hash params (alphabetical): uniqueReference only
        Map<String, Object> hashFields = new LinkedHashMap<>();
        hashFields.put("uniqueReference", request.uniqueReference());
        String hash = GeneralUtils.generateHashedKey(hashFields, props.getApiKey());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("uniqueReference", request.uniqueReference());
        form.add("hash",            hash);

        return logService.save(request.uniqueReference(), "OTT_CHECK_REMIT", url, form.toSingleValueMap())
                .flatMap(savedLog ->
                        post(url, form)
                                .doOnSuccess(raw -> log.info("OTT CheckRemitVoucher ref={}: {}", request.uniqueReference(), raw))
                                .flatMap(raw -> logService.updateSuccess(savedLog.getId(), "OK", raw)
                                        .thenReturn(parseCheckRemitVoucher(raw)))
                                .onErrorResume(ex -> logService.updateFailure(savedLog.getId(), ex.getMessage())
                                        .then(Mono.error(ex))));
    }

    // ── Shared HTTP helper ────────────────────────────────────────────────────

    private Mono<String> post(String url, MultiValueMap<String, String> form) {
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, basicAuth())
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("OTT error [" + resp.statusCode() + "]: " + body))))
                .bodyToMono(String.class);
    }

    // ── Response parsers ──────────────────────────────────────────────────────

    private OttCheckVoucherResponse parseCheckVoucher(String raw) {
        try {
            JsonNode n = objectMapper.readTree(raw);
            boolean ok = "true".equalsIgnoreCase(n.path("success").asText());
            return new OttCheckVoucherResponse(
                    ok,
                    n.path("serial").asText(null),
                    n.path("voucherID").asText(null),
                    n.path("value").asText(null),
                    n.path("message").asText(null),
                    n.path("errorCode").asText(null));
        } catch (Exception e) {
            log.warn("Failed to parse OTT CheckVoucher response: {}", e.getMessage());
            return new OttCheckVoucherResponse(false, null, null, null, "Parse error", null);
        }
    }

    private OttRemitVoucherResponse parseRemitVoucher(String raw) {
        try {
            JsonNode n = objectMapper.readTree(raw);
            boolean ok = "true".equalsIgnoreCase(n.path("success").asText());
            return new OttRemitVoucherResponse(
                    ok,
                    n.path("voucherID").asText(null),
                    n.path("voucherAmount").asText(null),
                    n.path("voucherBalance").asText(null),
                    n.path("errorCode").asText(null),
                    n.path("message").asText(null));
        } catch (Exception e) {
            log.warn("Failed to parse OTT RemitVoucher response: {}", e.getMessage());
            return new OttRemitVoucherResponse(false, null, null, null, null, "Parse error");
        }
    }

    private OttCheckRemitVoucherResponse parseCheckRemitVoucher(String raw) {
        try {
            JsonNode n = objectMapper.readTree(raw);
            boolean ok = "true".equalsIgnoreCase(n.path("success").asText());
            return new OttCheckRemitVoucherResponse(
                    ok,
                    n.path("voucherID").asText(null),
                    n.path("voucherAmount").asText(null),
                    n.path("voucherBalance").asText(null),
                    n.path("serialNumber").asText(null),
                    n.path("errorCode").asText(null),
                    n.path("message").asText(null));
        } catch (Exception e) {
            log.warn("Failed to parse OTT CheckRemitVoucher response: {}", e.getMessage());
            return new OttCheckRemitVoucherResponse(false, null, null, null, null, null, "Parse error");
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String basicAuth() {
        String credentials = props.getUsername() + ":" + props.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static String mask(String s) {
        return s.length() <= 4 ? "****" : s.substring(0, 4) + "****";
    }
}
