package net.tylersoft.common.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChannelDetails(

        @NotBlank(message = "channelDetails.deviceId is required")
        String deviceId,

        /** Human-readable device name or model, e.g. "Samsung Galaxy S23". */
        String name,

        /** OS version, e.g. "Android 14" or "iOS 17.2". */
        String osVersion,

        /** App semantic version, e.g. "1.4.2". */
        String appVersion,

        @NotBlank(message = "channelDetails.channel is required")
        @Pattern(
                regexp = "MOBILE|WEB|INTEGRATOR",
                message = "channelDetails.channel must be one of: MOBILE, WEB or INTEGRATOR"
        )
        String channel,

        /** Originating IP address — populated server-side if not supplied by client. */
        String ipAddress,

        /** OS family, e.g. "Android", "iOS". */
        String os

) {
}
