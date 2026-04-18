package net.tylersoft.payment.intercape;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.payment.intercape.api.*;
import net.tylersoft.payment.intercape.dto.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/intercape")
@RequiredArgsConstructor
public class IntercapeController {

    private final IntercapeService service;

    @PostMapping("/trips")
    public Mono<ApiResponse<IntercapeTripLookupResponse>> searchTrips(@RequestBody TripSearchRequest request) {
        return service.searchTrips(request).map(ApiResponse::ok);
    }

    @PostMapping("/booking")
    public Mono<ApiResponse<IntercapeBookingResponse>> booking(@RequestBody BookingApiRequest request) {
        return service.booking(request).map(ApiResponse::ok);
    }

    @PostMapping("/booking/total")
    public Mono<ApiResponse<IntercapeBookingTotalResponse>> bookingTotal(@RequestBody BookingTotalApiRequest request) {
        return service.bookingTotal(request).map(ApiResponse::ok);
    }

    @PostMapping("/booking/paid")
    public Mono<ApiResponse<IntercapeBookingPaidResponse>> bookingPaid(@RequestBody BookingPaidApiRequest request) {
        return service.bookingPaid(request).map(ApiResponse::ok);
    }

    @PostMapping("/payment-status")
    public Mono<ApiResponse<IntercapePaymentStatusResponse>> paymentStatus(@RequestBody PaymentStatusApiRequest request) {
        return service.updatePaymentStatus(request).map(ApiResponse::ok);
    }
}
