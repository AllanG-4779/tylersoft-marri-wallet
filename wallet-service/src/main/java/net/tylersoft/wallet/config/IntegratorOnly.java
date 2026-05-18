package net.tylersoft.wallet.config;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@integratorGuard.isIntegrator(authentication)")
public @interface IntegratorOnly {}
