package net.tylersoft.payment.intercape;

import lombok.RequiredArgsConstructor;
import net.tylersoft.payment.intercape.api.*;
import net.tylersoft.payment.intercape.dto.*;
import net.tylersoft.payment.intercape.dto.IntercapeBusStopResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IntercapeService {

    private final IntercapeClient client;

    public Mono<IntercapeTripLookupResponse> searchTrips(TripSearchRequest request) {
        return client.lookupTrips(request.depPlace(), request.arrPlace(), request.transactionId());
    }

    public Mono<IntercapeBookingResponse> booking(BookingApiRequest request) {
        return client.booking(request);
    }

    public Mono<IntercapeBookingTotalResponse> bookingTotal(BookingTotalApiRequest request) {
        return client.bookingTotal(request);
    }

    public Mono<IntercapeBookingPaidResponse> bookingPaid(BookingPaidApiRequest request) {
        return client.bookingPaid(request);
    }

    public Mono<IntercapeBusStopResponse> getBusStops() {
        return client.busStops();
    }

    public Mono<IntercapePaymentStatusResponse> updatePaymentStatus(PaymentStatusApiRequest request) {
        return client.paymentStatus(request.transactionId(), request.ticketSerial(), request.status());
    }
}
