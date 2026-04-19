package net.tylersoft.wallet.config;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.wallet.model.ChargeConfig;
import net.tylersoft.wallet.model.ServiceManagement;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v2/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    // ── Service Management ────────────────────────────────────────────────────

    @PostMapping("/services")
    public Mono<ApiResponse<ServiceManagement>> addServiceConfig(
            @RequestBody UniversalRequestWrapper<ServiceConfigRequest> request) {
        return configService.addServiceConfig(request.data())
                .map(ApiResponse::ok)
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    @GetMapping("/services")
    public Mono<ApiResponse<List<ServiceManagement>>> listServiceConfigs() {
        return configService.listServiceConfigs()
                .collectList()
                .map(ApiResponse::ok);
    }

    @GetMapping("/services/{id}")
    public Mono<ApiResponse<ServiceManagement>> getServiceConfig(@PathVariable Integer id) {
        return configService.getServiceConfig(id)
                .map(ApiResponse::ok)
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    @GetMapping("/services/by-code/{serviceCode}")
    public Mono<ApiResponse<ServiceManagement>> getServiceConfigByCode(
            @PathVariable String serviceCode) {
        return configService.getServiceConfigByCode(serviceCode)
                .map(ApiResponse::ok)
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    // ── Charge Config ─────────────────────────────────────────────────────────

    @PostMapping("/charges")
    public Mono<ApiResponse<ChargeConfig>> addChargeConfig(
            @RequestBody UniversalRequestWrapper<ChargeConfigRequest> request) {
        return configService.addChargeConfig(request.data())
                .map(ApiResponse::ok)
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    @GetMapping("/charges/service/{serviceManagementId}")
    public Mono<ApiResponse<List<ChargeConfig>>> listChargesByService(
            @PathVariable Integer serviceManagementId) {
        return configService.listChargesByService(serviceManagementId)
                .collectList()
                .map(ApiResponse::ok);
    }

    @GetMapping("/charges/{id}")
    public Mono<ApiResponse<ChargeConfig>> getChargeConfig(@PathVariable Integer id) {
        return configService.getChargeConfig(id)
                .map(ApiResponse::ok)
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
