package net.tylersoft.payment.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("trx_outgoing_requests")
@Getter
@Setter
@NoArgsConstructor
public class OutgoingRequestLog {

    @Id
    private Long id;

    private String referenceId;
    private String serviceCode;
    private String endpoint;
    private String requestPayload;
    private String responsePayload;
    private String responseCode;
    private String status;
    private String errorMessage;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
}
