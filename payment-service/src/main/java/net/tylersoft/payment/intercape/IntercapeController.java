package net.tylersoft.payment.intercape;

import lombok.RequiredArgsConstructor;
import java.util.List;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.payment.intercape.api.*;
import net.tylersoft.payment.intercape.dto.*;
import net.tylersoft.payment.intercape.dto.IntercapeBusStopResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/intercape")
@RequiredArgsConstructor
public class IntercapeController {

    private final IntercapeService service;

    @GetMapping("/bus-stops")
    public Mono<ApiResponse<List<IntercapeBusStopResponse.Stop>>> getBusStops() {
        return service.getBusStops()
                .map(resp -> {
                    if (resp.errorMessage() != null) {
                        return ApiResponse.<List<IntercapeBusStopResponse.Stop>>error(resp.errorMessage());
                    }
                    return ApiResponse.ok(resp.content().stops());
                });
    }

    @PostMapping("/trips")
    public Mono<ApiResponse<List<IntercapeTripLookupResponse.Trip>>> searchTrips(@RequestBody TripSearchRequest request) {
        return service.searchTrips(request)
                .map(resp -> {
                    if (resp.errorMessage() != null) {
                        return ApiResponse.error(resp.errorMessage());
                    }
                    if (resp.content() == null || resp.content().trips() == null) {
                        return ApiResponse.error("No trips available for the selected route");
                    }
                    return ApiResponse.ok(resp.content().trips());
                });
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
