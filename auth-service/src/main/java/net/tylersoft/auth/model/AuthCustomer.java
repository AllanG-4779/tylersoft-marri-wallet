package net.tylersoft.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(schema = "users", value = "customers")
@Getter
@NoArgsConstructor
public class AuthCustomer {

    @Id
    private UUID id;
    private String phoneNumber;
    private String pinHash;
    private String status;
}
