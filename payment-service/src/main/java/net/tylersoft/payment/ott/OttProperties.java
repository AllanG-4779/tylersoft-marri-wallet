package net.tylersoft.payment.ott;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ott")
@Data
public class OttProperties {

    private String baseUrl;
    private String voucherEndpoint = "/api/reseller/v1/GetVoucher";
    private String username;
    private String password;
    private String secretKey;
    private String merchantId;
    private String branch;
    private String cashier;
    private String till;
    private String vendorCode;

    // Redemption API
    private int    vendorId;
    private String apiKey;
    private String checkVoucherEndpoint    = "/api/v1/CheckVoucher";
    private String remitVoucherEndpoint    = "/api/v1/RemitVoucher";
    private String checkRemitVoucherEndpoint = "/api/v1/CheckRemitVoucher";
}
