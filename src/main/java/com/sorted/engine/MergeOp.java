package com.sorted.engine;

/** 合并操作：把 from 槽的 amount 个物品并入 to 槽（to 已装有同类物品）。 */
public record MergeOp(int from, int to, long amount) {
}
