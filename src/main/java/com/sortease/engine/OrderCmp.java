package com.sortease.engine;

/**
 * 排序模式中"物品排序键"的比较器（可与数量/名称/ID/模组结合）。
 * 返回 0 表示两者排序等价（可视为同一档位）。
 */
@FunctionalInterface
public interface OrderCmp<E> {
    int compare(E a, long aCount, E b, long bCount);
}
