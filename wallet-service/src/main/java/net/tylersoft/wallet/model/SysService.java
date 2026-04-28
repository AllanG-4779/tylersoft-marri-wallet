package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("sys_services")
@Getter @Setter @NoArgsConstructor
public class SysService {

    @Id
    private Integer id;

    private String transactionType;
    @Column("is_bill")
    private Boolean isBill;
    @Column("is_enquiry")
    private Boolean isEnquiry;
    private Short status;
    private String description;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
}
