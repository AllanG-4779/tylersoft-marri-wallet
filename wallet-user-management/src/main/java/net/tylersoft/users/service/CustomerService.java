package net.tylersoft.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.users.common.CustomerStatus;
import net.tylersoft.users.common.OtpPurpose;
import net.tylersoft.users.dto.*;
import net.tylersoft.users.model.Customer;
import net.tylersoft.users.model.IdentityDocument;
import net.tylersoft.users.client.WalletServiceClient;
import net.tylersoft.users.repository.CustomerRepository;
import net.tylersoft.users.repository.IdentityDocumentRepository;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final BCryptPasswordEncoder pinEncoder = new BCryptPasswordEncoder(10);

    private final CustomerRepository customerRepository;
    private final IdentityDocumentRepository identityDocumentRepository;
    private final OtpService otpService;
    private final FileStorageService fileStorageService;
    private final WalletServiceClient walletServiceClient;

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Creates a new customer record, stores the supplied ID document images, and
     * dispatches a registration OTP in one atomic onboarding call.
     *
     * <p>The customer starts in {@code INITIATED} status. Because documents are
     * uploaded at registration time the status advances to {@code DOCUMENTS_UPLOADED}
     * as soon as the OTP is verified (see {@link #verifyOtp}).
     *
     * @param request   the registration form data
     * @param idFront   front-side image of the identity document
     * @param idBack    back-side image (optional — passports may omit this)
     */
    public Mono<CustomerResponse> register(RegisterRequest request,
                                           FilePart idFront,
                                           FilePart idBack) {
        return Mono.zip(
                        customerRepository.existsByPhoneNumber(request.phoneNumber()),
                        customerRepository.existsByEmail(request.email())
                )
                .flatMap(exists -> {
                    if (exists.getT1())
                        return Mono.error(new IllegalArgumentException(
                                "Phone number already registered"));
                    if (exists.getT2())
                        return Mono.error(new IllegalArgumentException(
                                "Email address already registered"));

                    Customer customer = new Customer();
                    customer.setId(UUID.randomUUID());
                    customer.setFirstName(request.firstName());
                    customer.setLastName(request.lastName());
                    customer.setPhoneNumber(request.phoneNumber());
                    customer.setEmail(request.email());
                    customer.setStatus(CustomerStatus.INITIATED.name());
                    customer.setStatusChangedAt(OffsetDateTime.now());
                    customer.setCreatedAt(OffsetDateTime.now());
                    customer.setUpdatedAt(OffsetDateTime.now());
                    return customerRepository.save(customer);
                })
                .flatMap(saved -> saveIdentityDocument(saved.getId(), request, idFront, idBack)
                        .thenReturn(saved))
                .flatMap(saved ->
                        otpService.generateAndSend(saved.getId(), saved.getPhoneNumber(),
                                OtpPurpose.REGISTRATION.name())
                                .thenReturn(saved))
                .map(CustomerResponse::from);
    }

    private Mono<Void> saveIdentityDocument(UUID customerId, RegisterRequest request,
                                            FilePart idFront, FilePart idBack) {
        String subDir = customerId.toString();

        Mono<String> frontUrlMono = fileStorageService.store(idFront, subDir);
        Mono<String> backUrlMono = idBack != null
                ? fileStorageService.store(idBack, subDir)
                : Mono.just("");

        return Mono.zip(frontUrlMono, backUrlMono)
                .flatMap(urls -> {
                    IdentityDocument doc = new IdentityDocument();
                    doc.setId(UUID.randomUUID());
                    doc.setCustomerId(customerId);
                    doc.setIdType(request.idType());
                    doc.setIdNumber(request.idNumber());
                    doc.setFrontImageUrl(urls.getT1());
                    doc.setBackImageUrl(urls.getT2().isBlank() ? null : urls.getT2());
                    doc.setVerificationStatus("UPLOADED");
                    doc.setCreatedAt(OffsetDateTime.now());
                    doc.setUpdatedAt(OffsetDateTime.now());
                    return identityDocumentRepository.save(doc);
                })
                .then();
    }

    // ── OTP verification ──────────────────────────────────────────────────────

    public Mono<CustomerResponse> verifyOtp(VerifyOtpRequest request) {
        return customerRepository.findByPhoneNumber(request.phoneNumber())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Customer not found")))
                .flatMap(customer ->
                        otpService.verify(customer.getId(), request.otp(), request.purpose())
                                .flatMap(matched -> {
                                    if (!matched)
                                        return Mono.error(new IllegalArgumentException(
                                                "Invalid OTP. Please try again."));

                                    if (!OtpPurpose.REGISTRATION.name().equals(request.purpose())) {
                                        return customerRepository.save(customer);
                                    }

                                    // Documents were uploaded at registration — skip straight to DOCUMENTS_UPLOADED
                                    return identityDocumentRepository.findByCustomerId(customer.getId())
                                            .hasElements()
                                            .flatMap(hasDocs -> {
                                                customer.setStatus(hasDocs
                                                        ? CustomerStatus.DOCUMENTS_UPLOADED.name()
                                                        : CustomerStatus.PHONE_VERIFIED.name());
                                                customer.setStatusChangedAt(OffsetDateTime.now());
                                                customer.setUpdatedAt(OffsetDateTime.now());
                                                return customerRepository.save(customer);
                                            });
                                })
                )
                .map(CustomerResponse::from);
    }

    // ── Resend OTP ────────────────────────────────────────────────────────────

    public Mono<Void> resendOtp(ResendOtpRequest request) {
        return customerRepository.findByPhoneNumber(request.phoneNumber())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Customer not found")))
                .flatMap(customer ->
                        otpService.generateAndSend(customer.getId(), customer.getPhoneNumber(),
                                request.purpose()));
    }

    // ── KYC document upload ───────────────────────────────────────────────────

    /**
     * Persists the uploaded ID document images and creates an
     * {@code identity_documents} row. Advances the customer to
     * {@code DOCUMENTS_UPLOADED}.
     *
     * @param customerId  the customer's UUID
     * @param idType      e.g. NATIONAL_ID, PASSPORT, DRIVING_LICENSE
     * @param idNumber    the document number (optional at this stage)
     * @param frontImage  front-side image file part
     * @param backImage   back-side image file part (nullable for passports)
     */
    public Mono<DocumentResponse> uploadDocuments(UUID customerId,
                                                  String idType,
                                                  String idNumber,
                                                  FilePart frontImage,
                                                  FilePart backImage) {
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Customer not found: " + customerId)))
                .flatMap(customer -> {
                    String subDir = customerId.toString();

                    Mono<String> frontUrlMono = fileStorageService.store(frontImage, subDir);
                    Mono<String> backUrlMono = backImage != null
                            ? fileStorageService.store(backImage, subDir)
                            : Mono.just("");

                    return Mono.zip(frontUrlMono, backUrlMono)
                            .flatMap(urls -> {
                                IdentityDocument doc = new IdentityDocument();
                                doc.setId(UUID.randomUUID());
                                doc.setCustomerId(customerId);
                                doc.setIdType(idType);
                                doc.setIdNumber(idNumber);
                                doc.setFrontImageUrl(urls.getT1());
                                doc.setBackImageUrl(urls.getT2().isBlank() ? null : urls.getT2());
                                doc.setVerificationStatus("UPLOADED");
                                doc.setCreatedAt(OffsetDateTime.now());
                                doc.setUpdatedAt(OffsetDateTime.now());

                                return identityDocumentRepository.save(doc)
                                        .flatMap(saved -> {
                                            customer.setStatus(CustomerStatus.DOCUMENTS_UPLOADED.name());
                                            customer.setStatusChangedAt(OffsetDateTime.now());
                                            customer.setUpdatedAt(OffsetDateTime.now());
                                            return customerRepository.save(customer)
                                                    .thenReturn(saved);
                                        });
                            });
                })
                .map(DocumentResponse::from);
    }

    // ── Set PIN ───────────────────────────────────────────────────────────────

    public Mono<CustomerResponse> setPin(UUID customerId, SetPinRequest request) {
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Customer not found: " + customerId)))
                .flatMap(customer -> {
                    if (CustomerStatus.INITIATED.name().equals(customer.getStatus()))
                        return Mono.error(new IllegalArgumentException(
                                "Phone number must be verified before setting a PIN"));

                    customer.setPinHash(pinEncoder.encode(request.pin()));
                    customer.setUpdatedAt(OffsetDateTime.now());

                    boolean shouldActivate =
                            CustomerStatus.DOCUMENTS_UPLOADED.name().equals(customer.getStatus())
                            || CustomerStatus.KYC_VERIFIED.name().equals(customer.getStatus());

                    if (shouldActivate) {
                        customer.setStatus(CustomerStatus.ACTIVE.name());
                        customer.setStatusChangedAt(OffsetDateTime.now());
                    }

                    return customerRepository.save(customer)
                            .flatMap(saved -> {
                                if (!shouldActivate) return Mono.just(saved);

                                String accountName = saved.getFirstName() + " " + saved.getLastName();
                                return walletServiceClient
                                        .createWalletAccount(saved.getPhoneNumber(), accountName, "KES")
                                        .doOnSuccess(accountNo -> log.info(
                                                "Wallet account opened: customerId={} accountNo={}",
                                                customerId, accountNo))
                                        .onErrorResume(err -> {
                                            log.error("Wallet account creation failed for customerId={}: {}",
                                                    customerId, err.getMessage());
                                            return Mono.empty();
                                        })
                                        .thenReturn(saved);
                            });
                })
                .map(CustomerResponse::from);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    public Mono<CustomerResponse> getProfile(UUID customerId) {
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Customer not found: " + customerId)))
                .map(CustomerResponse::from);
    }

    public Flux<DocumentResponse> getDocuments(UUID customerId) {
        return identityDocumentRepository.findByCustomerId(customerId)
                .map(DocumentResponse::from);
    }
}
