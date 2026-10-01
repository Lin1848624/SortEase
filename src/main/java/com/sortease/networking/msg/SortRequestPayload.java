package com.sortease.networking.msg;

import com.sortease.SortedMod;
import com.sortease.server.SortJobManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 客户端 → 服务端：请求整理当前打开的容器。 */
public record SortRequestPayload(int containerId, int mode, boolean ascending,
                                 boolean includePlayerMain, boolean includeHotbar,
                                 int[] forceSort, int[] forceIgnore)
        implements CustomPacketPayload {

    /** 手动覆写数组的读取上限（防御坏包） */
    private static final int MAX_INDEXES = 4096;

    public static final CustomPacketPayload.Type<SortRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SortedMod.MODID, "sort_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SortRequestPayload> STREAM_CODEC =
            CustomPacketPayload.codec(SortRequestPayload::write, SortRequestPayload::read);

    public SortRequestPayload {
        forceSort = forceSort == null ? new int[0] : forceSort;
        forceIgnore = forceIgnore == null ? new int[0] : forceIgnore;
    }

    @Override
    public CustomPacketPayload.Type<SortRequestPayload> type() {
        return TYPE;
    }

    public static void write(SortRequestPayload msg, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeByte(msg.mode);
        buf.writeBoolean(msg.ascending);
        buf.writeBoolean(msg.includePlayerMain);
        buf.writeBoolean(msg.includeHotbar);
        writeIndexArray(msg.forceSort, buf);
        writeIndexArray(msg.forceIgnore, buf);
    }

    public static SortRequestPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int mode = buf.readByte();
        boolean ascending = buf.readBoolean();
        boolean includeMain = buf.readBoolean();
        boolean includeHotbar = buf.readBoolean();
        int[] forceSort = readIndexArray(buf);
        int[] forceIgnore = readIndexArray(buf);
        return new SortRequestPayload(containerId, mode, ascending, includeMain, includeHotbar, forceSort, forceIgnore);
    }

    private static void writeIndexArray(int[] arr, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(arr.length);
        for (int v : arr) buf.writeVarInt(v);
    }

    private static int[] readIndexArray(RegistryFriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), MAX_INDEXES);
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = buf.readVarInt();
        return arr;
    }

    /** 在服务端主线程执行。 */
    public static void handle(SortRequestPayload msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SortJobManager.onSortRequest(player, msg);
            }
        });
    }
}
