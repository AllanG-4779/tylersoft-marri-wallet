package net.tylersoft.users.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "users", value = "merchants")
@Getter @Setter @NoArgsConstructor
public class Merchant {

    @Id
    private UUID id;

    private String merchantCode;
    private String businessName;
    private String businessEmail;
    private String businessPhone;
    private String contactPersonName;
    private String contactPersonPhone;
    private String businessType;
    private String registrationNumber;
    private String taxNumber;
    private String address;
    private String status;
    private String statusReason;
    private String accountNumber;
    private String createdBy;
    private String approvedBy;
    private OffsetDateTime statusChangedAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
