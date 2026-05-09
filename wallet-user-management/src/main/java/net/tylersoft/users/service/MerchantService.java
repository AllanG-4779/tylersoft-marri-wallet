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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
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
        // 1. Encode — keep your existing hints
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // bump to H if adding a logo later
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        // 2. Render styled
        int modules = matrix.getWidth();
        float mod = (float) size / modules;

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);

        // Identify finder pattern zones so we skip them during dot drawing
        Set<String> finderZone = finderPatternModules(modules);

        // Dots
        for (int row = 0; row < modules; row++) {
            for (int col = 0; col < modules; col++) {
                if (!matrix.get(col, row)) continue;
                if (finderZone.contains(row + "," + col)) continue;
                drawRoundedDot(g, col * mod, row * mod, mod, new Color(0x1a1a22));
            }
        }

        // Styled corner squares (top-left, top-right, bottom-left)
        Color outerColor = new Color(0x7c6dfa);
        Color innerColor = new Color(0xfa6d9b);
        drawCornerSquare(g, 0,                  0,                  mod, outerColor, innerColor);
        drawCornerSquare(g, (modules - 7) * mod, 0,                  mod, outerColor, innerColor);
        drawCornerSquare(g, 0,                  (modules - 7) * mod, mod, outerColor, innerColor);

        g.dispose();

        // 3. Base64 encode — same as before
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

// ── Helpers ──────────────────────────────────────────────────────────────────

    private static void drawRoundedDot(Graphics2D g, float x, float y, float mod, Color color) {
        float pad = mod * 0.1f;
        float s   = mod - pad * 2;
        float arc = s * 0.9f;   // ~circle; reduce for softer squares
        g.setColor(color);
        g.fill(new RoundRectangle2D.Float(x + pad, y + pad, s, s, arc, arc));
    }

    private static void drawCornerSquare(Graphics2D g, float x, float y, float mod,
                                         Color outer, Color inner) {
        float total = 7 * mod;
        float outerArc = total * 0.28f;

        // Outer band
        g.setColor(outer);
        g.fill(new RoundRectangle2D.Float(x, y, total, total, outerArc, outerArc));

        // White cutout
        g.setColor(Color.WHITE);
        float cutSize = total - 2 * mod;
        g.fill(new RoundRectangle2D.Float(x + mod, y + mod, cutSize, cutSize, outerArc * 0.5f, outerArc * 0.5f));

        // Inner dot
        g.setColor(inner);
        float dotSize = 3 * mod;
        float dotX    = x + 2 * mod;
        float dotY    = y + 2 * mod;
        g.fill(new RoundRectangle2D.Float(dotX, dotY, dotSize, dotSize, dotSize * 0.45f, dotSize * 0.45f));
    }

    private static Set<String> finderPatternModules(int modules) {
        Set<String> zone = new HashSet<>();
        int[][] origins = {{0, 0}, {modules - 7, 0}, {0, modules - 7}};
        for (int[] o : origins)
            for (int r = 0; r < 7; r++)
                for (int c = 0; c < 7; c++)
                    zone.add((o[0] + r) + "," + (o[1] + c));
        return zone;
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
