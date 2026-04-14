package net.tylersoft.wallet.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("tb_gl_service_mapping")
@Getter @Setter @NoArgsConstructor
public class GlServiceMapping {

    @Id
    private Integer id;

    private String accountNumber;
    private String branchCode;
    private String bankCode;
    private String beneficiaryName;
    private Integer serviceId;
    private OffsetDateTime createdOn;
    private String createdBy;
}
