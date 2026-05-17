package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.events.dto.IdRequest;
import net.tylersoft.events.dto.tag.CreateTagRequest;
import net.tylersoft.events.dto.tag.TagResponse;
import net.tylersoft.events.service.TagService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping("/list")
    public Mono<ApiResponse<List<TagResponse>>> list() {
        return tagService.listAll().collectList().map(ApiResponse::ok);
    }

    @PostMapping("/get")
    public Mono<ApiResponse<TagResponse>> get(@RequestBody @Valid IdRequest req) {
        return tagService.getById(req.id()).map(ApiResponse::ok);
    }

    @PostMapping("/create")
    public Mono<ApiResponse<TagResponse>> create(@RequestBody @Valid CreateTagRequest req) {
        return tagService.create(req).map(ApiResponse::ok);
    }

    @PostMapping("/delete")
    public Mono<ApiResponse<Void>> delete(@RequestBody @Valid IdRequest req) {
        return tagService.delete(req.id()).thenReturn(ApiResponse.<Void>ok(null));
    }
}
