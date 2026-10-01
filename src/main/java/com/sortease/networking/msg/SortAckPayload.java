package com.sortease.networking.msg;

import com.sortease.SortedMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端 → 客户端：整理结果回执。 */
public record SortAckPayload(int code, String messageKey) implements CustomPacketPayload {
    /** 整理完成 */
    public static final int OK = 0;
    /** 无需整理（已有序/无合并项） */
    public static final int NO_CHANGE = 1;
    /** 任务被中止 */
    public static final int CANCELLED = 2;
    /** 该容器/环境不支持整理 */
    public static final int UNSUPPORTED = 3;
    /** 已有进行中的任务或参数非法 */
    public static final int BUSY = 4;

    public static final CustomPacketPayload.Type<SortAckPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SortedMod.MODID, "sort_ack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SortAckPayload> STREAM_CODEC =
            CustomPacketPayload.codec(SortAckPayload::write, SortAckPayload::read);

    public SortAckPayload {
        messageKey = messageKey == null ? "" : messageKey;
    }

    @Override
    public CustomPacketPayload.Type<SortAckPayload> type() {
        return TYPE;
    }

    public static void write(SortAckPayload msg, RegistryFriendlyByteBuf buf) {
        buf.writeByte(msg.code);
        buf.writeUtf(msg.messageKey, 128);
    }

    public static SortAckPayload read(RegistryFriendlyByteBuf buf) {
        return new SortAckPayload(buf.readByte(), buf.readUtf(128));
    }

    /** 在客户端主线程执行（专用服务端不会走到这里）。 */
    public static void handle(SortAckPayload msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.sortease.client.SortedClientNet.onAck(msg);
            }
        });
    }
}
