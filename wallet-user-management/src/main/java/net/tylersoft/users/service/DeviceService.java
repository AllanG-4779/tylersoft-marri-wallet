package net.tylersoft.users.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ChannelDetails;
import net.tylersoft.users.model.CustomerDevice;
import net.tylersoft.users.repository.CustomerDeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final CustomerDeviceRepository deviceRepository;

    /**
     * Called after a customer successfully sets their PIN.
     * If the device is already registered for this customer, it is set to ACTIVE
     * and lastSeenAt is refreshed. Otherwise a new device record is created.
     */
    public Mono<CustomerDevice> registerDevice(UUID customerId,
                                               ChannelDetails channelDetails) {
        return deviceRepository.findByCustomerIdAndDeviceId(customerId, channelDetails.deviceId())
                .flatMap(existing -> {
                    existing.setStatus("ACTIVE");
                    existing.setLastSeenAt(OffsetDateTime.now());
                    existing.setOsVersion(channelDetails.osVersion());
                    existing.setAppVersion(channelDetails.appVersion());
                    return deviceRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    CustomerDevice device = new CustomerDevice();
                    device.setCustomerId(customerId);
                    device.setDeviceId(channelDetails.deviceId());
                    device.setName(channelDetails.name());
                    device.setOsVersion(channelDetails.osVersion());
                    device.setDeviceType(channelDetails.channel());
                    device.setAppVersion(channelDetails.appVersion());
                    device.setStatus("ACTIVE");
                    device.setRegisteredAt(OffsetDateTime.now());
                    device.setLastSeenAt(OffsetDateTime.now());
                    return deviceRepository.save(device);
                }));
    }

    public Flux<CustomerDevice> getDevicesForCustomer(UUID customerId) {
        return deviceRepository.findByCustomerId(customerId);
    }

    public Mono<CustomerDevice> updateStatus(UUID customerId, UUID deviceId, String status) {
        return deviceRepository.findById(deviceId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found")))
                .flatMap(device -> {
                    if (!device.getCustomerId().equals(customerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Device does not belong to this customer"));
                    }
                    device.setStatus(status);
                    return deviceRepository.save(device);
                });
    }
}
