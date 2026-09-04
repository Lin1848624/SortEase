package com.sorted.server;

import com.sorted.classify.ContainerClassifier;
import com.sorted.classify.MenuProfile;
import com.sorted.networking.Networking;
import com.sorted.networking.msg.SortAckMessage;
import com.sorted.networking.msg.SortRequestMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 服务端整理任务管理与请求入口。 */
public final class SortJobManager {
    private static final Map<UUID, SortJob> JOBS = new ConcurrentHashMap<>();

    private SortJobManager() {}

    public static void sendAck(ServerPlayer player, int code, String key) {
        if (player == null) return;
        Networking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SortAckMessage(code, key));
    }

    public static void onSortRequest(ServerPlayer player, SortRequestMessage msg) {
        if (player == null) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != msg.containerId) {
            sendAck(player, SortAckMessage.UNSUPPORTED, "sorted.ack.no_container");
            return;
        }
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            sendAck(player, SortAckMessage.CANCELLED, "sorted.ack.cursor_blocked");
            return;
        }
        UUID id = player.getUUID();
        if (JOBS.containsKey(id)) {
            sendAck(player, SortAckMessage.BUSY, "");
            return;
        }
        MenuProfile profile = ContainerClassifier.classify(menu, player.getInventory());
        if (!profile.canSort()) {
            sendAck(player, SortAckMessage.UNSUPPORTED, "sorted.ack.unsupported");
            return;
        }
        JOBS.put(id, new SortJob(player, menu, profile,
                msg.includePlayerMain, msg.includeHotbar,
                msg.forceSort, msg.forceIgnore, msg.mode, msg.ascending));
    }

    /** 服务端每 tick 分片推进所有任务。 */
    public static void tickServer(MinecraftServer server) {
        if (server == null || JOBS.isEmpty()) return;
        List<UUID> done = null;
        for (Map.Entry<UUID, SortJob> e : JOBS.entrySet()) {
            if (e.getValue().tick()) {
                if (done == null) done = new ArrayList<>();
                done.add(e.getKey());
            }
        }
        if (done != null) {
            for (UUID u : done) JOBS.remove(u);
        }
    }

    public static boolean hasJobs() {
        return !JOBS.isEmpty();
    }
}
