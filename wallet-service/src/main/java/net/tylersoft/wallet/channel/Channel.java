package net.tylersoft.wallet.channel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("chn_channels")
@Getter @Setter @NoArgsConstructor
public class Channel {

    @Id
    private Integer id;

    private String channelName;
    private String clientId;
    private String channelKey;
    private String hostName;
    private String hostIp;
    private String description;
    private Short status;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
    private String updateBy;
    private OffsetDateTime deletedOn;
    private String deletedBy;
}
