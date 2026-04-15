package net.tylersoft.wallet.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("trx_transaction_charges_config")
@Getter @Setter @NoArgsConstructor
public class TransactionCharge {

    @Id
    private Integer id;
    private Long esbRef;
    private Integer chargeId;
    private String chargeType;
    private BigDecimal chargeValue;
    private BigDecimal amount;
    private String statusCode;
    private String statusMessage;
    private BigDecimal totalCharge;
}
