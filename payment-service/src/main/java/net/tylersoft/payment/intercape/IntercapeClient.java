package net.tylersoft.payment.intercape;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.ReactiveHttpClient;
import net.tylersoft.payment.intercape.api.BookingApiRequest;
import net.tylersoft.payment.intercape.api.BookingPaidApiRequest;
import net.tylersoft.payment.intercape.api.BookingTotalApiRequest;
import net.tylersoft.payment.intercape.dto.*;
import net.tylersoft.payment.service.OutgoingRequestLogService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IntercapeClient {

    private final IntercapeProperties props;
    private final ReactiveHttpClient httpClient;
    private final OutgoingRequestLogService logService;
    private final ObjectMapper objectMapper;

    public Mono<IntercapeTripLookupResponse> lookupTrips(String depPlace, String arrPlace, String transactionId) {
        var request = new IntercapeTripLookupRequest(
                "TripLookup", trace(), props.getClientId(), props.getServiceId(),
                props.getUsername(), props.getPassword(), transactionId, depPlace, arrPlace
        );
        String url = props.getBaseUrl();
        return logService.save(transactionId, "INTERCAPE_TRIP_LOOKUP", url, request)
                .flatMap(log -> httpClient.postRaw(url, request)
                        .map(json -> json.replace("\"Content\":\"\"", "\"Content\":null"))
                        .flatMap(json -> {
                            try {
                                return Mono.just(objectMapper.readValue(json, IntercapeTripLookupResponse.class));
                            } catch (Exception e) {
                                return Mono.<IntercapeTripLookupResponse>error(
                                        new RuntimeException("Failed to parse trip lookup response: " + e.getMessage()));
                            }
                        })
                        .flatMap(resp -> logService.updateSuccess(log.getId(), "200", resp).thenReturn(resp))
                        .onErrorResume(ex -> logService.updateFailure(log.getId(), ex.getMessage())
                                .then(Mono.error(ex))));
    }

    public Mono<IntercapeBookingResponse> booking(BookingApiRequest req) {
        var header = new IntercapeBookingRequest.Header("OneWay", "Booking", trace());
        var tripItem = new IntercapeBookingRequest.TripItem(
                req.travelClass(), req.depPlace(), req.arrPlace(),
                req.tripId(), req.coachSerial(), String.valueOf(req.numTickets()), req.price()
        );
        var content = new IntercapeBookingRequest.Content(
                props.getClientId(), props.getServiceId(),
                props.getUsername(), props.getPassword(),
                req.transactionId(), List.of(tripItem)
        );
        var request = new IntercapeBookingRequest(header, content, "");
        return send(req.transactionId(), "INTERCAPE_BOOKING", request, IntercapeBookingResponse.class);
    }

    public Mono<IntercapeBookingTotalResponse> bookingTotal(BookingTotalApiRequest req) {
        var header = new IntercapeBookingTotalRequest.Header("BookingTotalRequest", trace(), "OneWay");
        var passengers = req.passengers().stream()
                .map(p -> new IntercapeBookingTotalRequest.Passenger(
                        p.firstName(), p.lastName(), p.cellNo(), p.cellNo(),
                        "No", p.discount() != null ? p.discount() : "A1", p.date()
                )).toList();
        var tripItem = new IntercapeBookingTotalRequest.TripItem(
                req.travelClass(), req.tripId(), String.valueOf(req.numTickets()),
                req.depPlace(), req.arrPlace(), req.coachSerial(), req.price(), passengers
        );
        var content = new IntercapeBookingTotalRequest.Content(
                props.getServiceId(), req.transactionId(), req.basketId(),
                new IntercapeBookingTotalRequest.TripDetails(List.of(tripItem))
        );
        var request = new IntercapeBookingTotalRequest(header, content, "");
        return send(req.transactionId(), "INTERCAPE_BOOKING_TOTAL", request, IntercapeBookingTotalResponse.class);
    }

    public Mono<IntercapeBookingPaidResponse> bookingPaid(BookingPaidApiRequest req) {
        var header = new IntercapeBookingPaidRequest.Header("BookingPaid", trace(), "OneWay");
        var passengers = req.passengers().stream()
                .map(p -> new IntercapeBookingPaidRequest.Passenger(
                        p.discount() != null ? p.discount() : "A1",
                        p.firstName(), p.lastName(), p.cellNo(), p.cellNo(), "NO"
                )).toList();
        var tripItem = new IntercapeBookingPaidRequest.TripItem(
                req.travelClass(), req.depPlace(), req.arrPlace(),
                req.coachSerial(), req.numTickets(), req.tripId(), passengers, req.price()
        );
        var content = new IntercapeBookingPaidRequest.Content(
                req.basketId(), props.getServiceId(), req.transactionId(),
                req.purchFirstName(), req.purchLastName(),
                req.purchContact(), req.purchCell(), "",
                new IntercapeBookingPaidRequest.TripDetails(tripItem)
        );
        var request = new IntercapeBookingPaidRequest(header, content);
        return send(req.transactionId(), "INTERCAPE_BOOKING_PAID", request, IntercapeBookingPaidResponse.class);
    }

    public Mono<IntercapeBusStopResponse> busStops() {
        var request = new IntercapeBusStopRequest("BusStop", trace());
        return send("BUS_STOPS", "INTERCAPE_BUS_STOPS", request, IntercapeBusStopResponse.class);
    }

    public Mono<IntercapePaymentStatusResponse> paymentStatus(String transactionId, String ticketSerial, String status) {
        var request = new IntercapePaymentStatusRequest(
                "PaymentStatus", trace(), ticketSerial, status,
                props.getClientId(), props.getServiceId(),
                props.getUsername(), props.getPassword(), transactionId
        );
        return send(transactionId, "INTERCAPE_PAYMENT_STATUS", request, IntercapePaymentStatusResponse.class);
    }

    private <T, R> Mono<R> send(String transactionId, String serviceCode, T request, Class<R> responseType) {
        String url = props.getBaseUrl();
        return logService.save(transactionId, serviceCode, url, request)
                .flatMap(log -> httpClient.post(url, request, responseType)
                        .flatMap(resp -> logService.updateSuccess(log.getId(), "200", resp).thenReturn(resp))
                        .onErrorResume(ex -> logService.updateFailure(log.getId(), ex.getMessage())
                                .then(Mono.error(ex))));
    }

    private String trace() {
        return String.valueOf(System.currentTimeMillis());
    }
}
