package com.sorted.server;

import com.sorted.engine.InventoryView;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/**
 * 服务端全局视图：以「菜单槽列表位置」为索引的只读访问，供合并阶段使用。
 * 数量上限一律取“物品级最大堆叠”（不采用槽位名义上限，因为大堆叠环境下该值往往失效），
 * 槽位的真实限制由权威写入(Slot.set / handler insert)自动兜底。
 */
public final class McSlotView implements InventoryView<ItemStack> {
    private final AbstractContainerMenu menu;
    private final boolean[] sortable;
    private final BooleanSupplier[] takeable;

    public McSlotView(AbstractContainerMenu menu, boolean[] sortable, BooleanSupplier[] takeable) {
        this.menu = menu;
        this.sortable = sortable;
        this.takeable = takeable;
    }

    @Override
    public int size() {
        return menu.slots.size();
    }

    @Override
    public ItemStack itemAt(int i) {
        ItemStack s = menu.getSlot(i).getItem();
        return s.isEmpty() ? null : s;
    }

    @Override
    public long countAt(int i) {
        return menu.getSlot(i).getItem().getCount();
    }

    @Override
    public boolean sortable(int i) {
        return sortable[i];
    }

    @Override
    public boolean canPlace(int i, ItemStack item) {
        try {
            return menu.getSlot(i).mayPlace(item);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public boolean canExtract(int i) {
        return takeable == null || takeable[i] == null || takeable[i].getAsBoolean();
    }

    @Override
    public boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    @Override
    public long capacity(int i, ItemStack item) {
        ItemStack cur = menu.getSlot(i).getItem();
        long cap = item.getMaxStackSize();
        if (!cur.isEmpty()) cap = Math.max(cap, cur.getCount());
        return cap;
    }
}
