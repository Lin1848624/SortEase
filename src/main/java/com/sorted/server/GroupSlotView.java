package com.sorted.server;

import com.sorted.engine.InventoryView;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import java.util.List;

/**
 * 单一「同主体存储」子视图：重排阶段每个存储组一个实例，
 * 位置 0..size-1 映射到菜单槽列表中的下标，保证交换只发生在同一存储内部。
 */
final class GroupSlotView implements InventoryView<ItemStack> {
    private final AbstractContainerMenu menu;
    private final List<Integer> indices;

    GroupSlotView(AbstractContainerMenu menu, List<Integer> indices) {
        this.menu = menu;
        this.indices = indices;
    }

    @Override
    public int size() {
        return indices.size();
    }

    @Override
    public ItemStack itemAt(int i) {
        ItemStack s = menu.getSlot(indices.get(i)).getItem();
        return s.isEmpty() ? null : s;
    }

    @Override
    public long countAt(int i) {
        return menu.getSlot(indices.get(i)).getItem().getCount();
    }

    @Override
    public boolean sortable(int i) {
        return true;
    }

    @Override
    public boolean canPlace(int i, ItemStack item) {
        return menu.getSlot(indices.get(i)).mayPlace(item);
    }

    @Override
    public boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    @Override
    public long capacity(int i, ItemStack item) {
        // 同 McSlotView：以物品级最大堆叠为准，槽位名义上限由权威写入兜底
        var slot = menu.getSlot(indices.get(i));
        ItemStack cur = slot.getItem();
        long cap = item.getMaxStackSize();
        if (!cur.isEmpty()) cap = Math.max(cap, cur.getCount());
        return cap;
    }
}
