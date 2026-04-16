package net.tylersoft.users.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Saves uploaded files to local disk. Swap the implementation of {@link #store}
 * for an S3/object-storage adapter when deploying to production.
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Saves a {@link FilePart} into {@code <upload-dir>/<subDir>/} and returns
     * the relative URL path that can be stored in the database.
     *
     * @param file   the uploaded file part
     * @param subDir a sub-directory under the base upload dir (e.g. the customerId)
     * @return relative URL, e.g. {@code /uploads/<subDir>/<uuid>_<filename>}
     */
    public Mono<String> store(FilePart file, String subDir) {
        return Mono.fromCallable(() -> {
                    Path dir = baseDir.resolve(subDir);
                    Files.createDirectories(dir);
                    return dir;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(dir -> {
                    String filename = UUID.randomUUID() + "_" + sanitize(file.filename());
                    Path dest = dir.resolve(filename);
                    return DataBufferUtils.write(file.content(), dest)
                            .then(Mono.just("/uploads/" + subDir + "/" + filename));
                })
                .doOnSuccess(url -> log.debug("Stored file: {}", url))
                .doOnError(err -> log.error("Failed to store file: {}", file.filename(), err));
    }

    private String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void ensureBaseDir() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + baseDir, e);
        }
    }
}
