package com.sorted.engine;

/**
 * 排序引擎对"一组槽位"的只读视图（纯逻辑层，不依赖 Minecraft）。
 * 计数一律用 long，避免大堆叠场景下 int 相加溢出；适配层再收敛回 int。
 *
 * @param <E> 物品键类型（实现层通常用 (item, NBT) 组合）
 */
public interface InventoryView<E> {

    /** 槽位总数 */
    int size();

    /** 槽位 i 当前物品键；空槽返回 null */
    E itemAt(int i);

    /** 槽位 i 当前数量（itemAt(i)!=null 时 &gt;0） */
    long countAt(int i);

    /** 槽位 i 是否参与整理（特殊区域/被忽略槽返回 false） */
    boolean sortable(int i);

    /** 槽位 i 是否允许放入 item */
    boolean canPlace(int i, E item);

    /**
     * 两个物品是否“内容等价”（可合并）。
     * 默认按对象等价判断；MC 适配层应覆盖为 ItemStack 的内容比较，
     * 因为 ItemStack 的 equals 只是对象同一性。
     */
    default boolean sameItem(E a, E b) {
        return a == b || (a != null && a.equals(b));
    }

    /** 槽位 i 是否允许作为「被取出」的来源（服务端据此跳过锁定/不可取槽）。 */
    default boolean canExtract(int i) {
        return true;
    }

    /**
     * 槽位 i 对 item 的绝对容量上限（含当前已装数量）。
     * 由实现层按"物品当前最大堆叠 × 槽位上限"动态计算，禁止硬编码 64/16。
     */
    long capacity(int i, E item);
}
