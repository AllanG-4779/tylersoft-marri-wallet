package net.tylersoft.wallet.common;

public record ChannelDetails(
        String channelName,
        String clientId,
        String hostName,
        String hostIp
) {}
