package com.sortease.networking;

import com.sortease.networking.msg.SortAckPayload;
import com.sortease.networking.msg.SortRequestPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络载荷注册（NeoForge 1.21 的 CustomPacketPayload 机制，取代 Forge 的 SimpleChannel）。
 * 载荷类型用 {@link SortRequestPayload} / {@link SortAckPayload} 的 {@code TYPE} 声明，
 * 发送用 {@code PacketDistributor.sendToServer / sendToPlayer}。
 */
public final class Networking {
    /** 网络协议版本：两端不一致时 NeoForge 会拒绝握手 */
    public static final String PROTOCOL_VERSION = "1";

    private Networking() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(Networking::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // 客户端 → 服务端：整理请求（在主线程处理）
        registrar.playToServer(SortRequestPayload.TYPE, SortRequestPayload.STREAM_CODEC, SortRequestPayload::handle);
        // 服务端 → 客户端：整理回执（在主线程处理）
        registrar.playToClient(SortAckPayload.TYPE, SortAckPayload.STREAM_CODEC, SortAckPayload::handle);
    }
}
