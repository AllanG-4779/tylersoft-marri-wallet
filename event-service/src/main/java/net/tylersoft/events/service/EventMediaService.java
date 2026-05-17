package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.dto.media.AddMediaRequest;
import net.tylersoft.events.dto.media.MediaResponse;
import net.tylersoft.events.model.EventMedia;
import net.tylersoft.events.repository.EventMediaRepository;
import net.tylersoft.events.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventMediaService {

    private final EventMediaRepository mediaRepository;
    private final EventRepository eventRepository;

    public Flux<MediaResponse> listByEvent(UUID eventId) {
        return mediaRepository.findAllByEventIdOrderBySortOrderAsc(eventId).map(MediaResponse::from);
    }

    public Mono<MediaResponse> add(UUID eventId, AddMediaRequest req) {
        return eventRepository.existsById(eventId)
                .flatMap(exists -> {
                    if (!exists) return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
                    EventMedia media = new EventMedia();
                    media.setEventId(eventId);
                    media.setMediaType(req.mediaType());
                    media.setUrl(req.url());
                    media.setCaption(req.caption());
                    media.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
                    media.setCreatedAt(OffsetDateTime.now());
                    return mediaRepository.save(media);
                })
                .map(MediaResponse::from);
    }

    public Mono<Void> delete(UUID eventId, UUID mediaId) {
        return mediaRepository.findById(mediaId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")))
                .flatMap(media -> {
                    if (!media.getEventId().equals(eventId))
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
                    return mediaRepository.delete(media);
                });
    }
}
