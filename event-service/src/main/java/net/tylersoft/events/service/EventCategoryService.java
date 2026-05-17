package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.dto.category.CategoryResponse;
import net.tylersoft.events.dto.category.CreateCategoryRequest;
import net.tylersoft.events.dto.category.UpdateCategoryRequest;
import net.tylersoft.events.model.EventCategory;
import net.tylersoft.events.repository.EventCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventCategoryService {

    private final EventCategoryRepository categoryRepository;

    public Flux<CategoryResponse> listRoots() {
        return categoryRepository.findAllByParentIdIsNullOrderBySortOrderAsc()
                .map(CategoryResponse::from);
    }

    public Flux<CategoryResponse> listChildren(UUID parentId) {
        return categoryRepository.findAllByParentIdOrderBySortOrderAsc(parentId)
                .map(CategoryResponse::from);
    }

    public Mono<CategoryResponse> getById(UUID id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .map(CategoryResponse::from);
    }

    public Mono<CategoryResponse> create(CreateCategoryRequest req) {
        EventCategory cat = new EventCategory();
        cat.setParentId(req.parentId());
        cat.setName(req.name());
        cat.setSlug(req.slug());
        cat.setIconUrl(req.iconUrl());
        cat.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        cat.setActive(true);
        cat.setCreatedAt(OffsetDateTime.now());
        return categoryRepository.save(cat).map(CategoryResponse::from);
    }

    public Mono<CategoryResponse> update(UUID id, UpdateCategoryRequest req) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(cat -> {
                    if (req.name() != null) cat.setName(req.name());
                    if (req.slug() != null) cat.setSlug(req.slug());
                    if (req.iconUrl() != null) cat.setIconUrl(req.iconUrl());
                    if (req.sortOrder() != null) cat.setSortOrder(req.sortOrder());
                    if (req.active() != null) cat.setActive(req.active());
                    return categoryRepository.save(cat);
                })
                .map(CategoryResponse::from);
    }

    public Mono<Void> delete(UUID id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(categoryRepository::delete);
    }
}
