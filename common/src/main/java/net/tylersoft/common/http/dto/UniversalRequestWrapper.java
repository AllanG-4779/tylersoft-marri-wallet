package net.tylersoft.common.http.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UniversalRequestWrapper<T>(

        @Valid
        @NotNull(message = "data is required")
        T data,

        @Valid
        @NotNull(message = "channelDetails is required")
        ChannelDetails channelDetails

) {}
