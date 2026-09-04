package com.sortease.networking.msg;

import com.sortease.server.SortJobManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：请求整理当前打开的容器。 */
public class SortRequestMessage {
    public final int containerId;
    public final int mode;            // SortMode.ordinal()
    public final boolean ascending;
    public final boolean includePlayerMain;
    public final boolean includeHotbar;
    public final int[] forceSort;     // 手动改为「强制参与」的槽位索引
    public final int[] forceIgnore;   // 手动改为「忽略」的槽位索引

    public SortRequestMessage(int containerId, int mode, boolean ascending,
                              boolean includePlayerMain, boolean includeHotbar,
                              int[] forceSort, int[] forceIgnore) {
        this.containerId = containerId;
        this.mode = mode;
        this.ascending = ascending;
        this.includePlayerMain = includePlayerMain;
        this.includeHotbar = includeHotbar;
        this.forceSort = forceSort == null ? new int[0] : forceSort;
        this.forceIgnore = forceIgnore == null ? new int[0] : forceIgnore;
    }

    public static void encode(SortRequestMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeByte(msg.mode);
        buf.writeBoolean(msg.ascending);
        buf.writeBoolean(msg.includePlayerMain);
        buf.writeBoolean(msg.includeHotbar);
        writeIndexArray(msg.forceSort, buf);
        writeIndexArray(msg.forceIgnore, buf);
    }

    public static SortRequestMessage decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int mode = buf.readByte();
        boolean ascending = buf.readBoolean();
        boolean includeMain = buf.readBoolean();
        boolean includeHotbar = buf.readBoolean();
        int[] forceSort = readIndexArray(buf);
        int[] forceIgnore = readIndexArray(buf);
        return new SortRequestMessage(containerId, mode, ascending, includeMain, includeHotbar, forceSort, forceIgnore);
    }

    private static void writeIndexArray(int[] arr, FriendlyByteBuf buf) {
        buf.writeVarInt(arr.length);
        for (int v : arr) buf.writeVarInt(v);
    }

    private static int[] readIndexArray(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 4096); // 防御坏包
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = buf.readVarInt();
        return arr;
    }

    /** 在服务端线程执行。 */
    public static void handle(SortRequestMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null) {
                SortJobManager.onSortRequest(ctx.getSender(), msg);
            }
        });
        ctx.setPacketHandled(true);
    }
}
