package net.tylersoft.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bind MinIO connection settings via application.yml:
 *
 * <pre>
 * tylersoft:
 *   storage:
 *     minio:
 *       endpoint: http://localhost:9000
 *       access-key: minioadmin
 *       secret-key: minioadmin
 *       bucket: my-bucket
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "tylersoft.storage.minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}