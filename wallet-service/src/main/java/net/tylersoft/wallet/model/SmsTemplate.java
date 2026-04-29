package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("cfg_sms_templates")
@Getter @Setter @NoArgsConstructor
public class SmsTemplate {

    @Id private Integer id;
    private String transactionType;
    private String transactionCode;
    private String direction;
    private String status;
    private String template;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
}
