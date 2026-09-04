package com.sorted.server;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 对单个服务端菜单槽位的权威读写封装。
 * SlotItemHandler 槽位走 IItemHandler API；原版 Container 槽位用 slot.set / mayPlace / mayPickup。
 */
public final class SlotHandle {
    private final Slot slot;

    public SlotHandle(Slot slot) {
        this.slot = slot;
    }

    public Slot slot() {
        return slot;
    }

    public boolean isHandler() {
        return slot instanceof SlotItemHandler;
    }

    public IItemHandler handler() {
        return ((SlotItemHandler) slot).getItemHandler();
    }

    public int handlerIndex() {
        return slot.getContainerSlot();
    }

    public ItemStack getLive() {
        return slot.getItem();
    }

    public long count() {
        return slot.getItem().getCount();
    }

    public boolean isEmpty() {
        return slot.getItem().isEmpty();
    }

    public boolean mayPlace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        try {
            return slot.mayPlace(stack);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean mayPickup(Player player) {
        if (player == null) return true;
        try {
            return slot.mayPickup(player);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 权威整体写入（Slot.set 对两类槽位都已打通）；失败返回 false 表示该槽不可直写。
     */
    public boolean setWhole(ItemStack stack) {
        try {
            slot.set(stack);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** 权威移除 amount 个，返回实际移除的堆叠（可能少于 amount 或为空）。 */
    public ItemStack removePartial(long amount) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack cur = slot.getItem();
        if (cur.isEmpty()) return ItemStack.EMPTY;
        long take = Math.min(amount, cur.getCount());
        if (take <= 0) return ItemStack.EMPTY;
        if (isHandler()) {
            return handler().extractItem(handlerIndex(), (int) take, false);
        }
        ItemStack removed = cur.copy();
        removed.setCount((int) take);
        if (cur.getCount() == take) {
            slot.set(ItemStack.EMPTY);
        } else {
            ItemStack remain = cur.copy();
            remain.setCount(cur.getCount() - (int) take);
            slot.set(remain);
        }
        return removed;
    }

    /**
     * 权威插入（尊重槽位过滤与容量），返回无法放入的剩余数量。计数全用 long 防溢出。
     */
    public long insert(ItemStack toAdd) {
        if (toAdd == null || toAdd.isEmpty()) return 0;
        if (isHandler()) {
            ItemStack left = handler().insertItem(handlerIndex(), toAdd, false);
            return left.isEmpty() ? 0 : left.getCount();
        }
        ItemStack cur = slot.getItem();
        if (!cur.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(cur, toAdd)) return toAdd.getCount();
        } else if (!slot.mayPlace(toAdd)) {
            return toAdd.getCount();
        }
        long itemMax = toAdd.getMaxStackSize();
        long cap = itemMax;
        if (!cur.isEmpty()) cap = Math.max(cap, cur.getCount());
        long current = cur.getCount();
        if (current >= cap) return toAdd.getCount();
        long put = Math.min(toAdd.getCount(), cap - current);
        if (put <= 0) return toAdd.getCount();
        ItemStack result;
        if (cur.isEmpty()) {
            result = toAdd.copy();
            result.setCount((int) put);
        } else {
            result = cur.copy();
            result.setCount((int) (current + put));
        }
        slot.set(result);
        return toAdd.getCount() - put;
    }
}
