package net.tylersoft.common.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;

/**
 * Reusable MinIO upload service. Runs all blocking SDK calls on
 * {@link Schedulers#boundedElastic()} to stay non-blocking on the event loop.
 *
 * <p>Usage (inject {@code MinioService} in any service bean):
 * <pre>
 * // upload to the default bucket (tylersoft.storage.minio.bucket)
 * minioService.uploadFile("kyc/customer-123/id.pdf", pdfBytes, "application/pdf")
 *
 * // upload to an explicit bucket
 * minioService.uploadFile("documents", "kyc/customer-123/id.pdf", pdfBytes, "application/pdf")
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /**
     * Uploads content to the default bucket configured via
     * {@code tylersoft.storage.minio.bucket}.
     *
     * @param objectName  path/name of the object in the bucket (e.g. {@code "kyc/john/id.pdf"})
     * @param content     raw file bytes
     * @param contentType MIME type (e.g. {@code "application/pdf"}, {@code "image/jpeg"})
     * @return ETag of the uploaded object
     */
    public Mono<String> uploadFile(String objectName, FilePart content, String contentType) {
        return uploadFile(properties.getBucket(), objectName, content, contentType);
    }

    /**
     * Uploads content to the specified bucket, creating it if it does not exist.
     *
     * @param bucket      target bucket name
     * @param objectName  path/name of the object in the bucket
     * @param content     raw file bytes
     * @param contentType MIME type
     * @return ETag of the uploaded object
     */
    public Mono<String> uploadFile(String bucket, String objectName, FilePart content, String contentType)  {
        return DataBufferUtils.join(content.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return Mono.fromCallable(() -> {
                                ensureBucketExists(bucket);

                                var response = minioClient.putObject(
                                        PutObjectArgs.builder()
                                                .bucket(bucket)
                                                .object(objectName)
                                                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                                                .contentType(contentType)
                                                .build()
                                );

                                log.debug("Uploaded '{}' to bucket '{}', etag: {}", objectName, bucket, response.etag());
                                return response.etag();
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                })
            // wait up to 30s for the upload to complete

                .doOnError(err -> {
                    err.printStackTrace();
                    log.error("Failed to upload '{}' to bucket '{}': {}", objectName, bucket, err.getMessage());
                });
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            log.info("Bucket '{}' does not exist — creating it", bucket);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}