package net.tylersoft.wallet.currency;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("sys_currencies")
@Getter @Setter @NoArgsConstructor
public class Currency {

    @Id
    private Integer id;

    private String currencyName;
    private String currencyCode;
    private String isoCode;
    private Short status;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
}
