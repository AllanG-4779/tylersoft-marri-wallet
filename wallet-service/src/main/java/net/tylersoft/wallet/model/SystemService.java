package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("sys_services")
@Getter @Setter @NoArgsConstructor
public class SystemService {

    @Id
    private Integer id;

    private String transactionType;
    private Boolean isBill;
    private Boolean isEnquiry;
    private Short status;
    private String description;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
}
