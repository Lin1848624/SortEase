package com.sortease.networking.msg;

import com.sortease.client.SortedClientNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 → 客户端：整理结果回执。 */
public class SortAckMessage {
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

    public final int code;
    /** 翻译键或空串 */
    public final String messageKey;

    public SortAckMessage(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey == null ? "" : messageKey;
    }

    public static void encode(SortAckMessage msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.code);
        buf.writeUtf(msg.messageKey, 128);
    }

    public static SortAckMessage decode(FriendlyByteBuf buf) {
        return new SortAckMessage(buf.readByte(), buf.readUtf(128));
    }

    public static void handle(SortAckMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> SortedClientNet.onAck(msg)));
        ctx.setPacketHandled(true);
    }
}
