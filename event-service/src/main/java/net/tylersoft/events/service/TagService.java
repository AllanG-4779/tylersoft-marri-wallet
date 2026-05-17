package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.dto.tag.CreateTagRequest;
import net.tylersoft.events.dto.tag.TagResponse;
import net.tylersoft.events.model.Tag;
import net.tylersoft.events.repository.TagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public Flux<TagResponse> listAll() {
        return tagRepository.findAllByOrderByNameAsc().map(TagResponse::from);
    }

    public Mono<TagResponse> getById(UUID id) {
        return tagRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found")))
                .map(TagResponse::from);
    }

    public Mono<TagResponse> create(CreateTagRequest req) {
        return tagRepository.existsBySlug(req.slug())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Tag slug already exists"));
                    Tag tag = new Tag();
                    tag.setName(req.name());
                    tag.setSlug(req.slug());
                    tag.setCreatedAt(OffsetDateTime.now());
                    return tagRepository.save(tag);
                })
                .map(TagResponse::from);
    }

    public Mono<Void> delete(UUID id) {
        return tagRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found")))
                .flatMap(tagRepository::delete);
    }
}
