package net.tylersoft.payment.ott;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.payment.ott.dto.OttVoucherRequest;
import net.tylersoft.payment.ott.dto.OttVoucherResponse;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OttClient {

    private final OttProperties props;
    private final WebClient webClient;
    private final OutgoingRequestLogService logService;
    private final ObjectMapper objectMapper;

    public Mono<OttVoucherResponse> getVoucher(OttVoucherRequest request) {
        String reference = request.uniqueReference() != null
                ? request.uniqueReference()
                : UUID.randomUUID().toString();

        MultiValueMap<String, String> formData = buildFormData(request, reference);
        String url = props.getBaseUrl() + props.getVoucherEndpoint();

        return logService.save(reference, "OTT_VOUCHER", url, formData.toSingleValueMap())
                .flatMap(savedLog ->
                        webClient.post()
                                .uri(url)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .header(HttpHeaders.AUTHORIZATION, basicAuth())
                                .body(BodyInserters.fromMultipartData(formData))
                                .retrieve()
                                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                        resp -> resp.bodyToMono(String.class)
                                                .flatMap(body -> Mono.error(
                                                        new RuntimeException("OTT error [" + resp.statusCode() + "]: " + body))))
                                .bodyToMono(String.class)
                                .doOnSuccess(raw -> log.info("OTT response ref={}: {}", reference, raw))
                                .flatMap(raw -> logService
                                        .updateSuccess(savedLog.getId(), "OK", raw)
                                        .thenReturn(parseResponse(reference, raw)))
                                .onErrorResume(ex -> logService
                                        .updateFailure(savedLog.getId(), ex.getMessage())
                                        .then(Mono.error(ex)))
                );
    }

    private MultiValueMap<String, String> buildFormData(OttVoucherRequest request, String reference) {
        // OTT API field names differ from our DTO — map them here
        Map<String, Object> hashableFields = new LinkedHashMap<>();
        hashableFields.put("branch", props.getBranch());
        hashableFields.put("cashier", props.getCashier());
        hashableFields.put("merchantID", props.getMerchantId());
        hashableFields.put("mobileForSMS", request.phoneNumber());   // our: phoneNumber
        hashableFields.put("till", props.getTill());
        hashableFields.put("uniqueReference", reference);
        hashableFields.put("value", request.amount());               // our: amount
        hashableFields.put("vendorCode", props.getVendorCode());

        String hash = GeneralUtils.generateHashedKey(hashableFields, props.getSecretKey());
        log.debug("OTT hash computed for ref={}", reference);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("branch", props.getBranch());
        form.add("cashier", props.getCashier());
        form.add("merchantID", props.getMerchantId());
        form.add("mobileForSMS", request.phoneNumber());
        form.add("till", props.getTill());
        form.add("uniqueReference", reference);
        form.add("value", request.amount());
        form.add("vendorCode", props.getVendorCode());
        form.add("hash", hash);
        return form;
    }

    private OttVoucherResponse parseResponse(String reference, String raw) {
        try {
            JsonNode outer = objectMapper.readTree(raw);
            if (!outer.has("voucher")) {
                return new OttVoucherResponse(reference, null, null, null, null, null, null, raw);
            }
            // voucher field is itself a JSON string — parse it
            JsonNode voucher = objectMapper.readTree(outer.path("voucher").asText());
            return new OttVoucherResponse(
                    reference,
                    voucher.path("pin").asText(null),
                    voucher.path("serialNumber").asText(null),
                    voucher.has("voucherID") ? voucher.path("voucherID").asLong() : null,
                    voucher.path("batch").asText(null),
                    voucher.path("instructions").asText(null),
                    voucher.has("amount") ? BigDecimal.valueOf(voucher.path("amount").asDouble()) : null,
                    raw
            );
        } catch (Exception e) {
            log.warn("Failed to parse OTT response ref={}: {}", reference, e.getMessage());
            return new OttVoucherResponse(reference, null, null, null, null, null, null, raw);
        }
    }

    private String basicAuth() {
        String credentials = props.getUsername() + ":" + props.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
