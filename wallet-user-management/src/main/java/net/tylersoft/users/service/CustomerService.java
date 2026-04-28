package net.tylersoft.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.users.common.CustomerStatus;
import net.tylersoft.users.common.OtpPurpose;
import net.tylersoft.users.dto.*;
import net.tylersoft.users.model.Customer;
import net.tylersoft.users.model.IdentityDocument;
import net.tylersoft.users.client.WalletServiceClient;
import net.tylersoft.users.repository.CustomerRepository;
import net.tylersoft.users.repository.IdentityDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final DeviceService deviceService;
    private final TransactionalOperator transactionalOperator;
    @Value("${otp.demo.enabled:true}")
    private boolean demoEnabled;
    @Value("${otp.demo.value:636363}")
    private String demoOtp;


    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Creates a new customer record, stores the supplied ID document images, and
     * dispatches a registration OTP in one atomic onboarding call.
     *
     * <p>The customer starts in {@code INITIATED} status. Because documents are
     * uploaded at registration time the status advances to {@code DOCUMENTS_UPLOADED}
     * as soon as the OTP is verified (see {@link #verifyOtp}).
     *
     * @param request     the registration form data
     * @param webExchange the current web exchange (used to access multipart files)
     */
    public Mono<CustomerResponse> register(RegisterRequest request, ServerWebExchange webExchange) {
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
                .flatMap(saved -> saveIdentityDocument(saved.getId(), getFileParts(webExchange, request.idType()))
                        .thenReturn(saved)).as(transactionalOperator::transactional)
                .flatMap(saved ->
                        otpService.generateAndSend(saved.getId(), saved.getPhoneNumber(),
                                        OtpPurpose.REGISTRATION.name())
                                .thenReturn(saved))
                .map(CustomerResponse::from);
    }


    private Mono<Map<String, FilePart>> getFileParts(ServerWebExchange exchange, String idType) {
        boolean isPassport = idType.contains("PASSPORT");

        List<String> requiredKeys = isPassport
                ? List.of("passport", "selfie")
                : List.of("id_front", "id_back", "selfie");

        String errorMessage = isPassport
                ? "Passport image is required"
                : "id_front, id_back and selfie images are required";

        return exchange.getMultipartData()
                .flatMap(parts -> {
                    if (!parts.keySet().containsAll(requiredKeys)) {
                        return Mono.error(new IllegalArgumentException(errorMessage));
                    }
                    Map<String, FilePart> files = requiredKeys.stream()
                            .collect(Collectors.toMap(
                                    key -> key,
                                    key -> (FilePart) parts.getFirst(key)
                            ));
                    return Mono.just(files);
                });
    }

    private Mono<Void> saveIdentityDocument(UUID customerId, Mono<Map<String, FilePart>> filePartsMono) {
        return filePartsMono
                .flatMapMany(parts -> Flux.fromIterable(parts.entrySet()))
                .flatMap(entry -> fileStorageService.store(entry.getValue(), customerId.toString())
                        .flatMap(url -> {
                            IdentityDocument doc = new IdentityDocument();
                            doc.setCustomerId(customerId);
                            doc.setIdType(entry.getKey());
                            doc.setFrontImageUrl(url);
                            doc.setVerificationStatus("UPLOADED");
                            doc.setCreatedAt(OffsetDateTime.now());
                            doc.setUpdatedAt(OffsetDateTime.now());
                            return identityDocumentRepository.save(doc);
                        })
                )
                .then();
    }

    // ── OTP verification ──────────────────────────────────────────────────────

    public Mono<CustomerResponse> verifyOtp(VerifyOtpRequest request) {
        return customerRepository.findByPhoneNumber(request.phoneNumber())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Customer not found")))
                .flatMap(customer ->
                        otpService.verify(customer.getId(), request.otp(), request.purpose(), demoEnabled, demoOtp)
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


    // ── Set PIN ───────────────────────────────────────────────────────────────

    public Mono<CustomerResponse> setPin(UniversalRequestWrapper<SetPinRequest> requestWrapper) {
        var request = requestWrapper.data();
        var channelInfo = requestWrapper.channelDetails();
        return customerRepository.findById(request.customerId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Customer not found: " + request.customerId())))
                .filter(customer -> !CustomerStatus.INITIATED.name().equals(customer.getStatus()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Phone number must be verified before setting a PIN")))
                .flatMap(customer -> {
                    customer.setPinHash(pinEncoder.encode(request.pin()));
                    customer.setUpdatedAt(OffsetDateTime.now());

                    boolean shouldActivate = CustomerStatus.DOCUMENTS_UPLOADED.name().equals(customer.getStatus())
                            || CustomerStatus.KYC_VERIFIED.name().equals(customer.getStatus());

                    if (shouldActivate) {
                        customer.setStatus(CustomerStatus.ACTIVE.name());
                        customer.setStatusChangedAt(OffsetDateTime.now());
                    }

                    return customerRepository.save(customer)
                            .flatMap(saved -> shouldActivate ? createWalletFor(saved) : Mono.just(saved));
                })
                .flatMap(customer -> deviceService
                        .registerDevice(customer.getId(), channelInfo)
                        .thenReturn(customer))
                .map(CustomerResponse::from);
    }

    public Mono<Customer> createWalletFor(Customer customer) {
        String accountName = customer.getFirstName() + " " + customer.getLastName();
        return walletServiceClient
                .createWalletAccount(customer.getPhoneNumber(), accountName, "KES")
                .doOnSuccess(accountNo -> log.info(
                        "Wallet account opened: customerId={} accountNo={}", customer.getId(), accountNo))
                .thenReturn(customer) // ← keeps return type as Mono<Customer>
                .onErrorResume(err -> {
                    log.error("Wallet account creation failed for customerId={}: {}",
                            customer.getId(), err.getMessage());
                    customer.setStatus(CustomerStatus.WALLET_CREATION_FAILED.name());
                    customer.setStatusChangedAt(OffsetDateTime.now());
                    customer.setStatusReason(err.getMessage().substring(0, Math.min(err.getMessage().length(), 100)));
                    return customerRepository.save(customer);
                });
    }

    public Mono<CustomerResponse> getProfile(UUID customerId) {
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Customer not found: " + customerId)))
                .flatMap(customer ->
                        walletServiceClient.getAccountsByPhone(customer.getPhoneNumber())
                                .map(accounts -> CustomerResponse.from(customer, accounts))
                                .onErrorResume(err -> {
                                    log.warn("Failed to fetch accounts for customerId={}: {}", customerId, err.getMessage());
                                    return Mono.just(CustomerResponse.from(customer));
                                })
                );
    }


    public Mono<CustomerResponse> lookupByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Customer not found for phone: " + phoneNumber)))
                .flatMap(this::retryWalletIfNeeded)
                .flatMap(customer ->
                        walletServiceClient.getAccountsByPhone(customer.getPhoneNumber())
                                .map(accounts -> CustomerResponse.from(customer, accounts))
                                .onErrorResume(err -> {
                                    log.warn("Failed to fetch accounts for phone={}: {}", phoneNumber, err.getMessage());
                                    return Mono.just(CustomerResponse.from(customer));
                                })
                );
    }

    private Mono<Customer> retryWalletIfNeeded(Customer customer) {
        if (!CustomerStatus.WALLET_CREATION_FAILED.name().equals(customer.getStatus()))
            return Mono.just(customer);

        String accountName = customer.getFirstName() + " " + customer.getLastName();
        return walletServiceClient
                .createWalletAccount(customer.getPhoneNumber(), accountName, "KES")
                .flatMap(accountNo -> {
                    log.info("Wallet retry succeeded: customerId={} accountNo={}", customer.getId(), accountNo);
                    customer.setStatus(CustomerStatus.ACTIVE.name());
                    customer.setStatusChangedAt(OffsetDateTime.now());
                    customer.setStatusReason(null);
                    customer.setUpdatedAt(OffsetDateTime.now());
                    return customerRepository.save(customer);
                })
                .onErrorResume(err -> {
                    log.warn("Wallet retry failed for customerId={}: {}", customer.getId(), err.getMessage());
                    customer.setStatusReason("Wallet retry failed: " + err.getMessage().substring(0, Math.min(err.getMessage().length(), 100)));
                    customer.setUpdatedAt(OffsetDateTime.now());
                    return customerRepository.save(customer);
                });
    }

    public Flux<DocumentResponse> getDocuments(UUID customerId) {
        return identityDocumentRepository.findByCustomerId(customerId)
                .map(DocumentResponse::from);
    }
}
