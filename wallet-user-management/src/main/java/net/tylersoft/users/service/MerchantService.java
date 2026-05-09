package net.tylersoft.users.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.users.client.WalletServiceClient;
import net.tylersoft.users.common.MerchantStatus;
import net.tylersoft.users.dto.merchant.*;
import net.tylersoft.users.model.Merchant;
import net.tylersoft.users.model.MerchantDocument;
import net.tylersoft.users.repository.MerchantDocumentRepository;
import net.tylersoft.users.repository.MerchantRepository;
import net.tylersoft.common.http.dto.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantDocumentRepository documentRepository;
    private final WalletServiceClient walletServiceClient;
    private final FileStorageService fileStorageService;
    private final TransactionalOperator transactionalOperator;
    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    // ── Registration ──────────────────────────────────────────────────────────

    public Mono<MerchantResponse> register(MerchantRegistrationRequest req,
                                           String createdBy,
                                           ServerWebExchange exchange) {
        return Mono.zip(
                merchantRepository.existsByBusinessEmail(req.businessEmail()),
                merchantRepository.existsByBusinessPhone(req.businessPhone())
        ).flatMap(exists -> {
            if (exists.getT1())
                return Mono.error(new IllegalArgumentException("Business email already registered"));
            if (exists.getT2())
                return Mono.error(new IllegalArgumentException("Business phone already registered"));

            Merchant merchant = new Merchant();
            merchant.setBusinessName(req.businessName());
            merchant.setBusinessEmail(req.businessEmail());
            merchant.setBusinessPhone(req.businessPhone());
            merchant.setContactPersonName(req.contactPersonName());
            merchant.setContactPersonPhone(req.contactPersonPhone());
            merchant.setBusinessType(req.businessType());
            merchant.setRegistrationNumber(req.registrationNumber());
            merchant.setTaxNumber(req.taxNumber());
            merchant.setAddress(req.address());
            merchant.setStatus(MerchantStatus.PENDING_REVIEW.name());
            merchant.setCreatedBy(createdBy);
            merchant.setStatusChangedAt(OffsetDateTime.now());
            merchant.setCreatedAt(OffsetDateTime.now());
            merchant.setUpdatedAt(OffsetDateTime.now());
            return merchantRepository.save(merchant);
        })
        .flatMap(saved -> saveDocuments(saved.getId(), exchange).thenReturn(saved))
        .as(transactionalOperator::transactional)
        .map(MerchantResponse::from);
    }

    private Mono<Void> saveDocuments(UUID merchantId, ServerWebExchange exchange) {
        return exchange.getMultipartData()
                .flatMapMany(parts -> Flux.fromIterable(parts.entrySet())
                        .filter(e -> e.getValue() instanceof FilePart)
                        .flatMap(entry -> {
                            FilePart file = (FilePart) entry.getValue();
                            return fileStorageService.store(file, "merchants/" + merchantId)
                                    .flatMap(url -> {
                                        MerchantDocument doc = new MerchantDocument();
                                        doc.setMerchantId(merchantId);
                                        doc.setDocumentType(entry.getKey());
                                        doc.setDocumentUrl(url);
                                        doc.setVerificationStatus("UPLOADED");
                                        doc.setCreatedAt(OffsetDateTime.now());
                                        doc.setUpdatedAt(OffsetDateTime.now());
                                        return documentRepository.save(doc);
                                    });
                        })
                )
                .then();
    }

    // ── Public lookup ─────────────────────────────────────────────────────────

    public Mono<MerchantResponse> getByMerchantCode(String merchantCode) {
        return merchantRepository.findByMerchantCode(merchantCode)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant not found: " + merchantCode)))
                .map(MerchantResponse::from);
    }

    public Mono<MerchantResponse> getById(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant not found: " + merchantId)))
                .map(MerchantResponse::from);
    }

    // ── Admin: list ───────────────────────────────────────────────────────────

    public Mono<Page<MerchantResponse>> listAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Mono.zip(
                merchantRepository.findAllByOrderByCreatedAtDesc(pageable).map(MerchantResponse::from).collectList(),
                merchantRepository.count()
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<Page<MerchantResponse>> listByStatus(String status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Mono.zip(
                merchantRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable).map(MerchantResponse::from).collectList(),
                merchantRepository.countByStatus(status)
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    // ── Admin: approve ────────────────────────────────────────────────────────

    public Mono<MerchantResponse> approve(UUID merchantId, String approvedBy) {
        return merchantRepository.findById(merchantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant not found: " + merchantId)))
                .flatMap(merchant -> {
                    if (!MerchantStatus.PENDING_REVIEW.name().equals(merchant.getStatus()))
                        return Mono.error(new IllegalArgumentException(
                                "Merchant is not in PENDING_REVIEW status"));

                    return nextMerchantCode()
                            .flatMap(code -> {
                                merchant.setMerchantCode(code);
                                merchant.setStatus(MerchantStatus.ACTIVE.name());
                                merchant.setApprovedBy(approvedBy);
                                merchant.setApprovedAt(OffsetDateTime.now());
                                merchant.setStatusChangedAt(OffsetDateTime.now());
                                merchant.setUpdatedAt(OffsetDateTime.now());
                                return merchantRepository.save(merchant);
                            });
                })
                .flatMap(merchant -> openMerchantAccount(merchant)
                        .map(accountNumber -> {
                            merchant.setAccountNumber(accountNumber);
                            return merchant;
                        })
                        .flatMap(merchantRepository::save)
                        .onErrorResume(err -> {
                            log.error("Failed to open MA account for merchant {}: {}",
                                    merchant.getId(), err.getMessage());
                            return Mono.just(merchant);
                        })
                )
                .map(MerchantResponse::from);
    }

    private Mono<String> openMerchantAccount(Merchant merchant) {
        String accountName = merchant.getBusinessName();
        return walletServiceClient.createMerchantAccount(
                        merchant.getBusinessPhone(), accountName, "KES")
                .doOnSuccess(resp -> log.info(
                        "MA account opened: merchantId={} accountNo={}", merchant.getId(),
                        resp.data() != null ? resp.data().accountNo() : "unknown"))
                .map(resp -> resp.data() != null ? resp.data().accountNo() : null)
                .filter(acct -> acct != null && !acct.isBlank())
                .switchIfEmpty(Mono.error(new IllegalStateException("MA account creation returned empty account number")));
    }

    // ── Admin: reject ─────────────────────────────────────────────────────────

    public Mono<MerchantResponse> reject(UUID merchantId, String reason, String rejectedBy) {
        return merchantRepository.findById(merchantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant not found: " + merchantId)))
                .flatMap(merchant -> {
                    if (!MerchantStatus.PENDING_REVIEW.name().equals(merchant.getStatus()))
                        return Mono.error(new IllegalArgumentException(
                                "Merchant is not in PENDING_REVIEW status"));
                    merchant.setStatus(MerchantStatus.REJECTED.name());
                    merchant.setStatusReason(reason);
                    merchant.setApprovedBy(rejectedBy);
                    merchant.setStatusChangedAt(OffsetDateTime.now());
                    merchant.setUpdatedAt(OffsetDateTime.now());
                    return merchantRepository.save(merchant);
                })
                .map(MerchantResponse::from);
    }

    // ── Admin: suspend ────────────────────────────────────────────────────────

    public Mono<MerchantResponse> suspend(UUID merchantId, String reason, String suspendedBy) {
        return merchantRepository.findById(merchantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant not found: " + merchantId)))
                .flatMap(merchant -> {
                    if (!MerchantStatus.ACTIVE.name().equals(merchant.getStatus()))
                        return Mono.error(new IllegalArgumentException("Merchant is not ACTIVE"));
                    merchant.setStatus(MerchantStatus.SUSPENDED.name());
                    merchant.setStatusReason(reason);
                    merchant.setApprovedBy(suspendedBy);
                    merchant.setStatusChangedAt(OffsetDateTime.now());
                    merchant.setUpdatedAt(OffsetDateTime.now());
                    return merchantRepository.save(merchant);
                })
                .map(MerchantResponse::from);
    }

    // ── Admin: reactivate ─────────────────────────────────────────────────────

    public Mono<MerchantResponse> reactivate(UUID merchantId, String reactivatedBy) {
        return merchantRepository.findById(merchantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant not found: " + merchantId)))
                .flatMap(merchant -> {
                    if (!MerchantStatus.SUSPENDED.name().equals(merchant.getStatus()))
                        return Mono.error(new IllegalArgumentException("Merchant is not SUSPENDED"));
                    merchant.setStatus(MerchantStatus.ACTIVE.name());
                    merchant.setStatusReason(null);
                    merchant.setApprovedBy(reactivatedBy);
                    merchant.setStatusChangedAt(OffsetDateTime.now());
                    merchant.setUpdatedAt(OffsetDateTime.now());
                    return merchantRepository.save(merchant);
                })
                .map(MerchantResponse::from);
    }

    // ── QR code generation (dynamic — not persisted) ─────────────────────────

    public Mono<MerchantQrResponse> generateQr(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Merchant not found: " + merchantId)))
                .flatMap(merchant -> {
                    if (!MerchantStatus.ACTIVE.name().equals(merchant.getStatus()))
                        return Mono.error(new IllegalArgumentException(
                                "QR codes can only be generated for ACTIVE merchants"));
                    return buildQrCode(merchant);
                });
    }

    private Mono<MerchantQrResponse> buildQrCode(Merchant merchant) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("merchantCode", merchant.getMerchantCode());
                payload.put("merchantName", merchant.getBusinessName());
                payload.put("businessPhone", merchant.getBusinessPhone());
                if (merchant.getAccountNumber() != null)
                    payload.put("accountNumber", merchant.getAccountNumber());

                String qrData = objectMapper.writeValueAsString(payload);
                String imageBase64 = encodeToBase64Png(qrData, 300);

                return new MerchantQrResponse(
                        merchant.getMerchantCode(),
                        merchant.getBusinessName(),
                        merchant.getBusinessPhone(),
                        merchant.getAccountNumber(),
                        qrData,
                        imageBase64
                );
            } catch (Exception e) {
                throw new IllegalStateException("QR code generation failed: " + e.getMessage(), e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    private static String encodeToBase64Png(String content, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    // ── Merchant documents ────────────────────────────────────────────────────

    public Flux<MerchantDocument> getDocuments(UUID merchantId) {
        return documentRepository.findAllByMerchantId(merchantId);
    }

    // ── Sequence helper ───────────────────────────────────────────────────────

    private Mono<String> nextMerchantCode() {
        return databaseClient
                .sql("SELECT nextval('users.merchant_code_seq')")
                .map((row, meta) -> row.get(0, Long.class))
                .first()
                .map(n -> String.format("MER%05d", n));
    }
}
