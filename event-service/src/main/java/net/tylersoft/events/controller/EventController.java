package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.common.storage.FileExtensionExtractor;
import net.tylersoft.common.storage.MinioService;
import net.tylersoft.events.dto.IdRequest;
import net.tylersoft.events.dto.event.*;
import net.tylersoft.events.service.EventService;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final MinioService minioService;

    @PostMapping("/list")
    public Mono<ApiResponse<Page<EventResponse>>> list(@RequestBody ListEventsRequest req) {
        int page = req.page() != null ? req.page() : 0;
        int size = req.size() != null ? req.size() : 20;
        if (req.organizationId() != null)
            return eventService.listByOrganization(req.organizationId(), page, size).map(ApiResponse::ok);
        if (req.status() != null) return eventService.listByStatus(req.status(), page, size).map(ApiResponse::ok);
        return eventService.listAll(page, size).map(ApiResponse::ok);
    }

    @PostMapping("/get")
    public Mono<ApiResponse<EventResponse>> get(@RequestBody @Valid IdRequest req) {
        return eventService.getById(req.id()).map(ApiResponse::ok);
    }

    @PostMapping("/create")
    public Mono<ApiResponse<EventResponse>> create(
            @RequestPart("data") @Valid CreateEventRequest req,
            @RequestPart("banner") FilePart banner,
            @RequestPart("logo") FilePart logo) {
        return Mono.just(UUID.randomUUID())
                .flatMap(userId -> Mono.zip(upload(banner), upload(logo))
                        .flatMap(t -> {
                            log.info("Uploaded banner: {}, logo: {}", t.getT1(), t.getT2());
                            return eventService.create(req, t.getT1(), t.getT2(), userId).map(ApiResponse::ok);
                        }));
    }

    private Mono<String> upload(FilePart file) {
        String contentType = FileExtensionExtractor.getFileExtension(file.filename()).get("contentType");
        return minioService.uploadFile(UUID.randomUUID().toString(), file, contentType);
    }

    @PostMapping("/update")
    public Mono<ApiResponse<EventResponse>> update(@RequestBody @Valid UpdateEventRequest req) {
        return eventService.update(req.id(), req).map(ApiResponse::ok);
    }

    @PostMapping("/change-status")
    public Mono<ApiResponse<EventResponse>> changeStatus(
            @RequestBody @Valid EventStatusRequest req,
            @RequestHeader("X-User-Id") UUID userId) {
        return eventService.changeStatus(req.id(), req, userId).map(ApiResponse::ok);
    }

    @PostMapping("/add-tag")
    public Mono<ApiResponse<Void>> addTag(@RequestBody @Valid EventTagRequest req) {
        return eventService.addTag(req.eventId(), req.tagId()).thenReturn(ApiResponse.<Void>ok(null));
    }

    @PostMapping("/remove-tag")
    public Mono<ApiResponse<Void>> removeTag(@RequestBody @Valid EventTagRequest req) {
        return eventService.removeTag(req.eventId(), req.tagId()).thenReturn(ApiResponse.<Void>ok(null));
    }

    @PostMapping("/delete")
    public Mono<ApiResponse<Void>> delete(@RequestBody @Valid IdRequest req) {
        return eventService.delete(req.id()).thenReturn(ApiResponse.<Void>ok(null));
    }
}
