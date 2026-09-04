package com.sortease.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 重排阶段的有状态迭代器：每次调用在「当前真实视图」上重算目标顺序并给出一个
 * 能让无序前进的整堆交换，直到返回 null。每次成功交换后游标前进，操作数有界（&le; 槽数）。
 */
public final class ReorderState<E> {
    private static final class Entry<E> {
        final E item;
        final long count;
        Entry(E item, long count) {
            this.item = item;
            this.count = count;
        }
    }

    private final InventoryView<E> view;
    private final OrderCmp<E> cmp;
    private final int[] slots; // 参与排序的槽位（升序）
    private int cursor;

    public ReorderState(InventoryView<E> view, OrderCmp<E> cmp) {
        this.view = view;
        this.cmp = cmp;
        List<Integer> tmp = new ArrayList<>();
        for (int i = 0; i < view.size(); i++) {
            if (view.sortable(i)) tmp.add(i);
        }
        this.slots = tmp.stream().mapToInt(Integer::intValue).toArray();
    }

    /** 与某槽位"是否已就位"匹配：空槽匹配空槽，非空槽需物品相同且排序等价。 */
    private boolean matches(int slotIndex, E desired, long desiredCount) {
        E actual = view.itemAt(slotIndex);
        long actualCount = view.countAt(slotIndex);
        if (actual == null || actualCount <= 0) return desired == null;
        if (desired == null) return false;
        return (actual == desired || actual.equals(desired))
                && cmp.compare(actual, actualCount, desired, desiredCount) == 0;
    }

    /** 返回下一次交换；无更多无序槽位时返回 null。 */
    public SwapOp next() {
        int m = slots.length;
        if (m < 2 || cursor >= m) return null;

        List<Entry<E>> nonEmpty = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            E item = view.itemAt(slots[i]);
            long count = view.countAt(slots[i]);
            if (item != null && count > 0) nonEmpty.add(new Entry<>(item, count));
        }
        int nonEmptyCount = nonEmpty.size();

        // 稳定的目标序列：非空项按排序键升序排在前，空槽全部排最后
        nonEmpty.sort((x, y) -> cmp.compare(x.item, x.count, y.item, y.count));

        for (int k = cursor; k < m; k++) {
            Entry<E> desired = k < nonEmptyCount ? nonEmpty.get(k) : null;
            E desiredItem = desired == null ? null : desired.item;
            long desiredCount = desired == null ? 0 : desired.count;
            if (matches(slots[k], desiredItem, desiredCount)) {
                cursor = k + 1;
                continue;
            }
            if (desiredItem == null) {
                // 空槽应该到该位置，但当前却是物品（理论不会发生）；提前结束，交由下轮重算
                cursor = k + 1;
                continue;
            }
            for (int q = k + 1; q < m; q++) {
                E qItem = view.itemAt(slots[q]);
                if (qItem != null && view.countAt(slots[q]) > 0
                        && view.sameItem(qItem, desiredItem)
                        && cmp.compare(qItem, view.countAt(slots[q]), desiredItem, desiredCount) == 0) {
                    cursor = k + 1;
                    return new SwapOp(slots[k], slots[q]);
                }
            }
            // 目标键不在后方：防御性结束，交由下一轮重算
            cursor = k + 1;
            return null;
        }
        cursor = m;
        return null;
    }

    /** 排序是否已全部完成（供服务端判定）。 */
    public boolean done() {
        return cursor >= slots.length;
    }
}
