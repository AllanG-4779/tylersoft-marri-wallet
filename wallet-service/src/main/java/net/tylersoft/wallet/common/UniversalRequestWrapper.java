package net.tylersoft.wallet.common;

public record UniversalRequestWrapper<T>(
        T data,
        ChannelDetails channelDetails
) {}
