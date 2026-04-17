package net.tylersoft.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "net.tylersoft.users",
                "net.tylersoft.common"
        })
public class WalletUserManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(WalletUserManagementApplication.class, args);
    }
}
