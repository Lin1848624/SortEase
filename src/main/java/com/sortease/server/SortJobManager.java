package com.sortease.server;

import com.sortease.SortedMod;
import com.sortease.classify.ContainerClassifier;
import com.sortease.classify.MenuProfile;
import com.sortease.networking.Networking;
import com.sortease.networking.msg.SortAckMessage;
import com.sortease.networking.msg.SortRequestMessage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端整理任务管理与请求入口。
 *
 * 为了杜绝“已经整理过、再按整理键仍然挪动物品”的体验问题，这里对每个玩家记录
 * 「上次整理完成后的容器状态签名」：下一次请求到来时若同一容器、同一菜单的内容/布局
 * 签名完全一致，则直接判定无需整理，不再创建任务（零操作、零广播）。
 */
public final class SortJobManager {
    private static final Map<UUID, SortJob> JOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, Entry> LAST_STATE = new ConcurrentHashMap<>();

    /** 某次整理结束后该容器的内容/布局签名 + 那次请求的参数。 */
    private static final class Entry {
        final int containerId;
        final String menuClass;
        final long sig;
        final int mode;
        final boolean ascending;
        final boolean includePlayerMain;
        final boolean includeHotbar;
        final int[] forceSort;
        final int[] forceIgnore;

        Entry(int containerId, String menuClass, long sig, SortRequestMessage msg) {
            this.containerId = containerId;
            this.menuClass = menuClass;
            this.sig = sig;
            this.mode = msg.mode;
            this.ascending = msg.ascending;
            this.includePlayerMain = msg.includePlayerMain;
            this.includeHotbar = msg.includeHotbar;
            this.forceSort = msg.forceSort.clone();
            this.forceIgnore = msg.forceIgnore.clone();
        }

        boolean matches(AbstractContainerMenu menu, long currentSig, SortRequestMessage msg) {
            return containerId == menu.containerId
                    && menuClass.equals(menu.getClass().getName())
                    && sig == currentSig
                    && mode == msg.mode
                    && ascending == msg.ascending
                    && includePlayerMain == msg.includePlayerMain
                    && includeHotbar == msg.includeHotbar
                    && Arrays.equals(forceSort, msg.forceSort)
                    && Arrays.equals(forceIgnore, msg.forceIgnore);
        }
    }

    private SortJobManager() {
    }

    public static void sendAck(ServerPlayer player, int code, String key) {
        if (player == null)
            return;
        Networking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SortAckMessage(code, key));
    }

    /** 对菜单全部槽位按“位置 × (物品id × 数量 × NBT哈希)”计算签名；任何变动都会改变它。 */
    private static long signatureOf(AbstractContainerMenu menu) {
        long h = 0x9E3779B97F4A7C15L;
        for (Slot s : menu.slots) {
            long v = 0;
            ItemStack it = s.getItem();
            if (!it.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(it.getItem());
                String name = id == null ? "<unknown>" : id.toString();
                v = name.hashCode();
                v = v * 31 + it.getCount();
                long tagH = it.getTag() == null ? 0 : it.getTag().hashCode();
                v = v * 31 + tagH;
            }
            h ^= v;
            h *= 0x100000001B3L;
        }
        return h;
    }

    public static void onSortRequest(ServerPlayer player, SortRequestMessage msg) {
        if (player == null)
            return;
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
        // 状态签名去重：同一容器、同一整理参数，且自上次整理后内容/布局无任何变化 → 直接静默跳过
        Entry prev = LAST_STATE.get(id);
        long sig = signatureOf(menu);
        if (prev != null && prev.matches(menu, sig, msg)) {
            sendAck(player, SortAckMessage.NO_CHANGE, "");
            return;
        }
        MenuProfile profile = ContainerClassifier.classify(menu, player.getInventory());
        if (!profile.canSort()) {
            sendAck(player, SortAckMessage.UNSUPPORTED, "sorted.ack.unsupported");
            return;
        }
        JOBS.put(id, new SortJob(player, menu, profile, msg));
    }

    /** 服务端每 tick 分片推进所有任务。 */
    public static void tickServer(MinecraftServer server) {
        if (server == null || JOBS.isEmpty())
            return;
        List<UUID> done = null;
        for (Map.Entry<UUID, SortJob> e : JOBS.entrySet()) {
            if (e.getValue().tick()) {
                if (done == null)
                    done = new ArrayList<>();
                done.add(e.getKey());
            }
        }
        if (done != null) {
            for (UUID u : done) {
                SortJob job = JOBS.remove(u);
                if (job != null)
                    recordFinishedState(u, job);
            }
        }
    }

    /**
     * 任务结束后记录该容器最新状态。仅当任务真正达到“整洁终态”且玩家仍停留在同一容器时记录；
     * 半途中止(超时/光标占用/中途关闭)或未达整洁态的任务一律移除旧记录，保证下次按键照常判断，
     * 避免把一次不完整的整理误当成“无需整理”而吞掉后续修正。
     */
    private static void recordFinishedState(UUID uuid, SortJob job) {
        if (job == null || !job.finishedClean()) {
            LAST_STATE.remove(uuid);
            return;
        }
        ServerPlayer p = job.player();
        if (p == null || p.containerMenu == null || p.containerMenu.containerId != job.containerId()) {
            // 容器已关闭或切换：移除旧记录，下次在新容器上照常判断
            LAST_STATE.remove(uuid);
            return;
        }
        LAST_STATE.put(uuid, new Entry(job.containerId(), job.menuClassName(),
                signatureOf(p.containerMenu), job.request()));
    }

    public static boolean hasJobs() {
        return !JOBS.isEmpty();
    }
}
