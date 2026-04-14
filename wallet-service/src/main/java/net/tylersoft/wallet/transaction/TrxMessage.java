package net.tylersoft.wallet.transaction;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("trx_messages")
@Getter @Setter @NoArgsConstructor
public class TrxMessage {

    @Id
    private Long id;

    private String transactionRef;
    private String channelTimestamp;
    private String channelReference;
    private String channelIp;
    private String geolocation;
    private String userAgent;
    private String userAgentVersion;
    private String channel;
    private String clientId;
    private String transactionCode;
    private String transactionType;
    private String hostCode;
    private String direction;
    private BigDecimal amount;
    private BigDecimal totalCharge;
    private String phoneNumber;
    private String debitAccount;
    private String creditAccount;
    private String billAccountNumber;
    private String responseCode;
    private String responseMessage;
    private Short serviceStatus;
    private String recipientPhoneNumber;
    private String merchantCode;
    private String agentCode;
    private String currency;
    private Short reversed;
    private String receiptNumber;
    private String callbackAddress;
    private OffsetDateTime reversedAt;
    private Integer serviceManagementId;
    private Short status;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
}
