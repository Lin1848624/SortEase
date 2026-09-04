package com.sorted.server;

import com.sorted.SortedMod;
import com.sorted.classify.ContainerClassifier;
import com.sorted.classify.MenuProfile;
import com.sorted.config.ServerConfig;
import com.sorted.engine.MergeOp;
import com.sorted.engine.OrderCmp;
import com.sorted.engine.ReorderState;
import com.sorted.engine.SortEngine;
import com.sorted.engine.SwapOp;
import com.sorted.networking.msg.SortAckMessage;
import com.sorted.sort.SortMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/** 一次「一键整理」的服务端任务：按 tick 分片执行合并 + 同存储内重排，全程权威校验。 */
public final class SortJob {
    private final ServerPlayer player;
    private final int containerId;
    private final String menuClass;
    private final SortMode mode;
    private final boolean ascending;
    private final boolean[] mask;
    private final BooleanSupplier[] takeable;
    private final int maskCount;

    private boolean mergesDone;
    private boolean change;
    private int totalOps;
    private int ticks;
    private int consecutiveNoChange;
    private int appliedMerges;
    private int appliedSwaps;
    private List<GroupState> groupStates;

    private static final class GroupState {
        final List<Integer> slots;
        ReorderState<ItemStack> state;
        boolean blocked;

        GroupState(List<Integer> slots) {
            this.slots = slots;
        }
    }

    public SortJob(ServerPlayer player, AbstractContainerMenu menu, MenuProfile profile,
                   boolean includePlayerMain, boolean includeHotbar,
                   int[] forceSort, int[] forceIgnore,
                   int modeOrdinal, boolean ascending) {
        this.player = player;
        this.containerId = menu.containerId;
        this.menuClass = menu.getClass().getName();
        this.mode = SortMode.byOrdinal(modeOrdinal);
        this.ascending = ascending;

        int n = menu.slots.size();
        this.mask = new boolean[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            boolean base;
            switch (profile.kind(i)) {
                case MAIN -> base = true;
                case PLAYER_MAIN -> base = includePlayerMain;
                case PLAYER_HOTBAR -> base = includeHotbar;
                default -> base = false;
            }
            this.mask[i] = base;
            if (base) count++;
        }
        if (forceSort != null) for (int i : forceSort) if (i >= 0 && i < n && !mask[i]) {
            mask[i] = true;
            count++;
        }
        if (forceIgnore != null) for (int i : forceIgnore) if (i >= 0 && i < n && mask[i]) {
            mask[i] = false;
            count--;
        }
        this.maskCount = Math.max(0, count);

        this.takeable = new BooleanSupplier[n];
        for (int i = 0; i < n; i++) {
            SlotHandle handle = new SlotHandle(menu.getSlot(i));
            this.takeable[i] = () -> handle.mayPickup(player);
        }
    }

    /* ---------- 排序键比较器 ---------- */

    private static String displayOf(ItemStack s) {
        try {
            return s.getHoverName().getString().toLowerCase(Locale.ROOT);
        } catch (RuntimeException ex) {
            return pathOf(s);
        }
    }

    private static String pathOf(ItemStack s) {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(s.getItem());
        return loc == null ? "" : loc.getPath();
    }

    private static String namespaceOf(ItemStack s) {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(s.getItem());
        return loc == null ? "" : loc.getNamespace();
    }

    private static OrderCmp<ItemStack> comparator(SortMode mode, boolean ascending) {
        return (a, ac, b, bc) -> {
            int r;
            switch (mode) {
                case COUNT -> r = Long.compare(bc, ac);
                case MOD -> r = namespaceOf(a).compareTo(namespaceOf(b));
                case ID -> r = pathOf(a).compareTo(pathOf(b));
                default -> r = String.CASE_INSENSITIVE_ORDER.compare(displayOf(a), displayOf(b));
            }
            if (r == 0) r = pathOf(a).compareTo(pathOf(b));
            if (r == 0) r = namespaceOf(a).compareTo(namespaceOf(b));
            return ascending ? r : -r;
        };
    }

    /* ---------- tick 推进 ---------- */

    private boolean menuAlive() {
        return player.containerMenu != null && player.containerMenu.containerId == containerId;
    }

    /** 执行一个 tick 内的分片；返回 true 表示任务结束（应从管理器移除）。 */
    public boolean tick() {
        AbstractContainerMenu menu = player.containerMenu;
        ticks++;
        if (!menuAlive()) return true;
        if (!menu.getCarried().isEmpty()) {
            SortJobManager.sendAck(player, SortAckMessage.CANCELLED, "sorted.ack.cursor_blocked");
            return true;
        }
        if (ticks > ServerConfig.maxJobTicks || totalOps > ServerConfig.hardMaxOps) {
            SortJobManager.sendAck(player, SortAckMessage.CANCELLED, "sorted.ack.timeout");
            return true;
        }

        int budget = ServerConfig.maxOpsPerTick;
        boolean anyThisTick = false;

        if (!mergesDone) {
            McSlotView view = new McSlotView(menu, mask, takeable);
            while (budget > 0) {
                MergeOp op = SortEngine.findMerge(view);
                if (op == null) {
                    mergesDone = true;
                    break;
                }
                budget--;
                totalOps++;
                if (applyMerge(menu, op)) {
                    anyThisTick = true;
                    change = true;
                    appliedMerges++;
                    consecutiveNoChange = 0;
                } else {
                    if (++consecutiveNoChange >= 16) {
                        mergesDone = true; // 保守终止，防个别怪异槽位导致空转
                        break;
                    }
                }
            }
        }
        // 合并可能在本 tick 内就已收敛：剩余预算继续执行重排（此前该分支永远走不到）
        if (mergesDone) {
            runReorderPhase(menu, budget);
        }

        if (anyThisTick) menu.broadcastChanges();
        if (mergesDone && reorderDone()) {
            finish(menu);
            return true;
        }
        return false;
    }

    private void finish(AbstractContainerMenu menu) {
        if (change) {
            SortJobManager.sendAck(player, SortAckMessage.OK, "");
            SortedMod.LOGGER.info("[Sorted] done menu={} mode={} maskSlots={} merges={} swaps={} ops={} ticks={}",
                    menuClass, mode, maskCount, appliedMerges, appliedSwaps, totalOps, ticks);
        } else if (maskCount == 0) {
            SortJobManager.sendAck(player, SortAckMessage.NO_CHANGE, "sorted.ack.no_sortable");
            SortedMod.LOGGER.info("[Sorted] nothing sortable menu={} mode={} maskSlots=0", menuClass, mode);
        } else {
            SortJobManager.sendAck(player, SortAckMessage.NO_CHANGE, "");
            SortedMod.LOGGER.info("[Sorted] no change menu={} mode={} maskSlots={} ops={} ticks={} {}", menuClass, mode,
                    maskCount, totalOps, ticks, structureSummary(menu));
        }
    }

    /** 诊断用：可整理槽里的非空数量、各存储组大小、以及首批物品样例。 */
    private String structureSummary(AbstractContainerMenu menu) {
        StringBuilder sb = new StringBuilder();
        int nonEmpty = 0;
        IdentityHashMap<Object, Integer> groups = new IdentityHashMap<>();
        java.util.List<String> samples = new ArrayList<>();
        int shown = 0;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (!mask[i]) continue;
            Object key = ContainerClassifier.storageKeyOf(menu.getSlot(i));
            groups.merge(key, 1, Integer::sum);
            ItemStack st = menu.getSlot(i).getItem();
            if (!st.isEmpty()) {
                nonEmpty++;
                if (shown < 4) {
                    shown++;
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(st.getItem());
                    String idStr = id == null ? "<null-key>" : id.toString();
                    String hover;
                    try {
                        hover = st.getHoverName().getString();
                    } catch (RuntimeException ex) {
                        hover = "<hover-err>";
                    }
                    samples.add(idStr + "(" + hover + ")x" + st.getCount());
                }
            }
        }
        sb.append("nonEmptyMaskSlots=").append(nonEmpty).append(" groups=[");
        boolean first = true;
        for (int g : groups.values()) {
            if (!first) sb.append(',');
            sb.append(g);
            first = false;
        }
        sb.append("] samples=").append(samples);
        return sb.toString();
    }

    private boolean reorderDone() {
        if (groupStates == null) return true;
        for (GroupState gs : groupStates) {
            if (!gs.blocked && gs.state != null && !gs.state.done()) return false;
        }
        return true;
    }

    private boolean runReorderPhase(AbstractContainerMenu menu, int budget) {
        boolean applied = false;
        if (groupStates == null) groupStates = buildGroups(menu);
        if (mode == SortMode.MERGE || groupStates.isEmpty()) return false;

        for (GroupState gs : groupStates) {
            if (gs.blocked || budget <= 0) continue;
            if (gs.state == null) {
                if (gs.slots.size() >= 2) {
                    gs.state = new ReorderState<>(new GroupSlotView(menu, gs.slots), comparator(mode, ascending));
                }
                continue;
            }
            SwapOp sw;
            while (budget > 0 && (sw = gs.state.next()) != null) {
                budget--;
                totalOps++;
                if (applySwap(menu, gs.slots.get(sw.a()), gs.slots.get(sw.b()))) {
                    change = true;
                    applied = true;
                    appliedSwaps++;
                } else {
                    gs.blocked = true; // 该存储内存在不可互放的槽（如过滤槽），放弃本组重排
                    break;
                }
            }
        }
        return applied;
    }

    /* ---------- 原子操作 ---------- */

    private boolean applyMerge(AbstractContainerMenu menu, MergeOp op) {
        SlotHandle src = new SlotHandle(menu.getSlot(op.from()));
        SlotHandle dst = new SlotHandle(menu.getSlot(op.to()));
        ItemStack s = src.getLive();
        if (s.isEmpty()) return false;
        ItemStack d = dst.getLive();
        if (!d.isEmpty() && !ItemStack.isSameItemSameTags(s, d)) return false;
        if (!dst.mayPlace(s)) return false;

        ItemStack removed = src.removePartial(op.amount());
        if (removed.isEmpty()) return false;
        long left = dst.insert(removed);
        boolean moved = left < removed.getCount();
        if (left > 0) {
            ItemStack back = removed.copy();
            back.setCount((int) Math.min(left, Integer.MAX_VALUE));
            src.insert(back);
        }
        return moved;
    }

    private boolean applySwap(AbstractContainerMenu menu, int aIdx, int bIdx) {
        SlotHandle sa = new SlotHandle(menu.getSlot(aIdx));
        SlotHandle sb = new SlotHandle(menu.getSlot(bIdx));
        if ((sa.isHandler() && !(sa.handler() instanceof net.minecraftforge.items.IItemHandlerModifiable))
                || (sb.isHandler() && !(sb.handler() instanceof net.minecraftforge.items.IItemHandlerModifiable))) {
            return false;
        }
        ItemStack va = sa.getLive().copy();
        ItemStack vb = sb.getLive().copy();
        if (!sa.mayPlace(vb) || !sb.mayPlace(va)) return false;
        if (!sa.setWhole(vb)) return false;
        if (!sb.setWhole(va)) return false;
        return true;
    }

    private List<GroupState> buildGroups(AbstractContainerMenu menu) {
        IdentityHashMap<Object, List<Integer>> map = new IdentityHashMap<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            if (!mask[i]) continue;
            Object key = ContainerClassifier.storageKeyOf(menu.getSlot(i));
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }
        List<GroupState> out = new ArrayList<>();
        for (List<Integer> idxs : map.values()) {
            if (idxs.size() >= 2) out.add(new GroupState(idxs));
        }
        return out;
    }
}
