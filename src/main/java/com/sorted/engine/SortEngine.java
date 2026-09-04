package com.sorted.engine;

/**
 * 合并阶段：每次从当前状态推导出「下一个应执行的合并操作」。
 * 引擎在服务端按 tick 分片调用（每次调用前从真实槽位刷新视图），天然抗外部变化且必然终止：
 * 每次成功合并都使某个参与槽位的装入量严格 +1，且扫描方向固定、不产生回退。
 */
public final class SortEngine {
    private SortEngine() {}

    /**
     * 找出下一个待合并操作：
     * 从左到右找第一个「仍有空间、且其后方存在同键堆」的槽位，把后方最近的同键堆并入它。
     *
     * @return 合并操作；没有可合并项时返回 null
     */
    public static <E> MergeOp findMerge(InventoryView<E> view) {
        int n = view.size();
        for (int i = 0; i < n; i++) {
            if (!view.sortable(i)) continue;
            E item = view.itemAt(i);
            if (item == null) continue;
            if (!view.canPlace(i, item)) continue;
            long count = view.countAt(i);
            long space = view.capacity(i, item) - count;
            if (space <= 0) continue;
            for (int j = i + 1; j < n; j++) {
                if (!view.sortable(j)) continue;
                if (!view.canExtract(j)) continue;
                E other = view.itemAt(j);
                if (other == null || countAt(view, j) <= 0) continue;
                if (view.sameItem(item, other)) {
                    long amount = Math.min(countAt(view, j), space);
                    if (amount > 0) {
                        return new MergeOp(j, i, amount);
                    }
                }
            }
        }
        return null;
    }

    private static <E> long countAt(InventoryView<E> view, int i) {
        return view.countAt(i);
    }
}
