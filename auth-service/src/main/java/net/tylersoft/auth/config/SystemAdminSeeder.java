package net.tylersoft.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.auth.model.AuthAdmin;
import net.tylersoft.auth.repository.AuthAdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemAdminSeeder implements ApplicationRunner {

    private final AuthAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        adminRepository.existsByUsername(adminUsername)
                .filter(exists -> !exists)
                .flatMap(ignored -> {
                    AuthAdmin admin = new AuthAdmin();
                    admin.setUsername(adminUsername);
                    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                    admin.setStatus("ACTIVE");
                    admin.setEnabled(true);
                    admin.setActive(true);
                    admin.setFirstLogin(false);
                    admin.setFailedLoginAttempts(0);
                    admin.setCreatedOn(OffsetDateTime.now());
                    admin.setUpdatedOn(OffsetDateTime.now());
                    return adminRepository.save(admin);
                })
                .doOnSuccess(admin -> {
                    if (admin != null) log.info("Seeded system administrator: {}", adminUsername);
                })
                .subscribe();
    }
}
