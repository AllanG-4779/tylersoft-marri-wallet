package net.tylersoft.users.controller;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.users.dto.DeviceResponse;
import net.tylersoft.users.dto.UpdateDeviceStatusRequest;
import net.tylersoft.users.service.DeviceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/users/{customerId}/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public Mono<ApiResponse<List<DeviceResponse>>> listDevices(@PathVariable UUID customerId) {
        return deviceService.getDevicesForCustomer(customerId)
                .map(DeviceResponse::from)
                .collectList()
                .map(ApiResponse::ok);
    }

    @PatchMapping("/{deviceId}/status")
    public Mono<ApiResponse<DeviceResponse>> updateStatus(
            @PathVariable UUID customerId,
            @PathVariable UUID deviceId,
            @Validated @RequestBody UpdateDeviceStatusRequest request) {
        return deviceService.updateStatus(customerId, deviceId, request.status())
                .map(DeviceResponse::from)
                .map(ApiResponse::ok);
    }
}
