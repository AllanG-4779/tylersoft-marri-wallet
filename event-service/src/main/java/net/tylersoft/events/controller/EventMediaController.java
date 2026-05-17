package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.events.dto.media.AddMediaRequest;
import net.tylersoft.events.dto.media.DeleteMediaRequest;
import net.tylersoft.events.dto.media.ListMediaRequest;
import net.tylersoft.events.dto.media.MediaResponse;
import net.tylersoft.events.service.EventMediaService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/event-media")
@RequiredArgsConstructor
public class EventMediaController {

    private final EventMediaService mediaService;

    @PostMapping("/list")
    public Mono<ApiResponse<List<MediaResponse>>> list(@RequestBody @Valid ListMediaRequest req) {
        return mediaService.listByEvent(req.eventId()).collectList().map(ApiResponse::ok);
    }

    @PostMapping("/add")
    public Mono<ApiResponse<MediaResponse>> add(@RequestBody @Valid AddMediaRequest req) {
        return mediaService.add(req.eventId(), req).map(ApiResponse::ok);
    }

    @PostMapping("/delete")
    public Mono<ApiResponse<Void>> delete(@RequestBody @Valid DeleteMediaRequest req) {
        return mediaService.delete(req.eventId(), req.mediaId()).thenReturn(ApiResponse.<Void>ok(null));
    }
}
