package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.events.dto.IdRequest;
import net.tylersoft.events.dto.category.CategoryResponse;
import net.tylersoft.events.dto.category.CreateCategoryRequest;
import net.tylersoft.events.dto.category.ListCategoriesRequest;
import net.tylersoft.events.dto.category.UpdateCategoryRequest;
import net.tylersoft.events.service.EventCategoryService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class EventCategoryController {

    private final EventCategoryService categoryService;

    @PostMapping("/list")
    public Mono<ApiResponse<List<CategoryResponse>>> list(@RequestBody ListCategoriesRequest req) {
        var results = req.parentId() == null
                ? categoryService.listRoots()
                : categoryService.listChildren(req.parentId());
        return results.collectList().map(ApiResponse::ok);
    }

    @PostMapping("/get")
    public Mono<ApiResponse<CategoryResponse>> get(@RequestBody @Valid IdRequest req) {
        return categoryService.getById(req.id()).map(ApiResponse::ok);
    }

    @PostMapping("/create")
    public Mono<ApiResponse<CategoryResponse>> create(@RequestBody @Valid CreateCategoryRequest req) {
        return categoryService.create(req).map(ApiResponse::ok);
    }

    @PostMapping("/update")
    public Mono<ApiResponse<CategoryResponse>> update(@RequestBody @Valid UpdateCategoryRequest req) {
        return categoryService.update(req.id(), req).map(ApiResponse::ok);
    }

    @PostMapping("/delete")
    public Mono<ApiResponse<Void>> delete(@RequestBody @Valid IdRequest req) {
        return categoryService.delete(req.id()).thenReturn(ApiResponse.<Void>ok(null));
    }
}
