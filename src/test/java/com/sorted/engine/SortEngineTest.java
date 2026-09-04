package com.sorted.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 纯逻辑整理引擎测试：大堆叠防溢出、合并终止、重排正确性、特殊槽不被触碰。 */
class SortEngineTest {

    /** 内存版 InventoryView（物品键为 String）。 */
    private static final class Model implements InventoryView<String> {
        final int n;
        final String[] items;
        final long[] counts;
        final boolean[] sortable;
        final long itemMax;       // 全局物品上限（可覆盖）
        final long slotLimit;     // 槽位上限（0 = 跟随物品上限）

        Model(int n, long itemMax, long slotLimit) {
            this.n = n;
            this.itemMax = itemMax;
            this.slotLimit = slotLimit;
            this.items = new String[n];
            this.counts = new long[n];
            this.sortable = new boolean[n];
            Arrays.fill(this.sortable, true);
        }

        Model set(int i, String item, long count) {
            items[i] = item;
            counts[i] = count;
            return this;
        }

        Model nonsortable(int i) {
            sortable[i] = false;
            return this;
        }

        @Override public int size() { return n; }
        @Override public String itemAt(int i) { return items[i]; }
        @Override public long countAt(int i) { return counts[i]; }
        @Override public boolean sortable(int i) { return sortable[i]; }
        @Override public boolean canPlace(int i, String item) { return true; }
        @Override public long capacity(int i, String item) {
            long max = itemMax;
            if (slotLimit > 0) max = Math.min(max, slotLimit);
            return max;
        }
    }

    private static void applyMerge(Model m, MergeOp op) {
        assertTrue(op.amount() > 0);
        assertTrue(op.from() != op.to());
        assertEquals(m.items[op.from()], m.items[op.to()]);
        m.counts[op.from()] -= op.amount();
        m.counts[op.to()] += op.amount();
        if (m.counts[op.from()] == 0) m.items[op.from()] = null; // 源槽清空后视为空槽
        assertTrue(m.counts[op.from()] >= 0);
        assertTrue(m.counts[op.to()] <= m.capacity(op.to(), m.items[op.to()]));
    }

    private static void applySwap(Model m, SwapOp op) {
        String ti = m.items[op.a()];
        long tc = m.counts[op.a()];
        m.items[op.a()] = m.items[op.b()];
        m.counts[op.a()] = m.counts[op.b()];
        m.items[op.b()] = ti;
        m.counts[op.b()] = tc;
    }

    @Test
    void basicMergeConsolidatesDuplicates() {
        Model m = new Model(4, 64, 0)
                .set(0, "X", 20).set(1, "X", 20).set(2, "X", 20).set(3, "Y", 5);
        int guard = 0;
        MergeOp op;
        while ((op = SortEngine.findMerge(m)) != null && guard++ < 100) applyMerge(m, op);
        assertEquals(60, m.counts[0]);
        assertEquals(0, m.counts[1]);
        assertEquals(0, m.counts[2]);
        assertEquals(5, m.counts[3]);
    }

    @Test
    void mergeDoesNotTouchNonSortableSlots() {
        Model m = new Model(5, 64, 0)
                .set(0, "X", 20).set(1, "X", 20)
                .nonsortable(2).set(2, "X", 60) // 特殊槽里也有一堆 X
                .set(3, "X", 20).set(4, "X", 20);
        int guard = 0;
        MergeOp op;
        while ((op = SortEngine.findMerge(m)) != null && guard++ < 200) {
            assertTrue(m.sortable[op.from()] && m.sortable[op.to()], "不得动用特殊槽");
            applyMerge(m, op);
        }
        // 槽 2 的特殊 X 保持 60 不动
        assertEquals(60, m.counts[2]);
    }

    @Test
    void hugeStacksDoNotOverflow() {
        // 模拟 1,073,741,823 级大堆叠（用户场景）
        long cap = 2_147_483_647L; // Integer.MAX_VALUE
        Model m = new Model(8, cap, 0)
                .set(0, "A", 1_073_741_823L).set(1, "A", 1_073_741_823L)
                .set(2, "A", 1_073_741_823L).set(3, "A", 1_073_741_823L)
                .set(4, "B", 999_999_999L).set(5, "B", 999_999_999L);
        int guard = 0;
        MergeOp op;
        while ((op = SortEngine.findMerge(m)) != null && guard++ < 200) {
            assertTrue(op.amount() > 0);
            applyMerge(m, op);
        }
        long totalA = 0, totalB = 0;
        for (int i = 0; i < m.n; i++) {
            assertTrue(m.counts[i] >= 0);
            if (m.items[i] != null) {
                assertTrue(m.counts[i] <= cap, "不得超过容量: " + m.counts[i]);
                if (m.items[i].equals("A")) totalA += m.counts[i];
                else totalB += m.counts[i];
            }
        }
        assertEquals(4_294_967_292L, totalA); // 4 × 1,073,741,823（远超 int，必须用 long 守恒）
        assertEquals(1_999_999_998L, totalB);
    }

    @Test
    void mergeTerminatesOnRandomModel() {
        Random rnd = new Random(42);
        for (int trial = 0; trial < 20; trial++) {
            Model m = new Model(120, 64, 0);
            for (int i = 0; i < 120; i++) {
                int kind = i % 7;
                if (i % 13 != 0) m.set(i, "T" + kind, 1 + rnd.nextInt(63));
                if (i % 17 == 0) m.nonsortable(i);
            }
            int guard = 0;
            MergeOp op;
            while ((op = SortEngine.findMerge(m)) != null && guard++ < 5000) applyMerge(m, op);
            assertTrue(guard < 5000, "合并必须终止");
            // 终止条件：不存在 i<j 同键、i 仍有空间
            for (int i = 0; i < 120; i++) {
                if (!m.sortable[i] || m.items[i] == null) continue;
                long space = m.capacity(i, m.items[i]) - m.counts[i];
                if (space <= 0) continue;
                for (int j = i + 1; j < 120; j++) {
                    if (m.sortable[j] && m.items[j] != null
                            && m.items[j].equals(m.items[i]) && m.counts[j] > 0) {
                        fail("合并未收敛: i=" + i + " j=" + j);
                    }
                }
            }
        }
    }

    private static OrderCmp<String> byName() {
        return (a, ac, b, bc) -> a.compareTo(b);
    }

    @Test
    void reorderSortsByNameAndLeavesEmptiesLast() {
        Model m = new Model(6, 64, 0)
                .set(0, "C", 3).set(1, "A", 2).set(2, "B", 1)
                .set(3, "A", 4); // A 有两堆（无法合并场景：数量不同也允许相邻）
        ReorderState<String> rs = new ReorderState<>(m, byName());
        int guard = 0;
        SwapOp s;
        while ((s = rs.next()) != null && guard++ < 20) applySwap(m, s);
        String[] items = new String[6];
        for (int i = 0; i < 6; i++) items[i] = m.items[i] == null ? "" : m.items[i];
        assertArrayEquals(new String[]{"A", "A", "B", "C", "", ""}, items, "应按名称升序，空槽靠后");
    }

    @Test
    void reorderNeverTouchesNonSortable() {
        Model m = new Model(5, 64, 0)
                .nonsortable(1).set(1, "Z", 99)
                .set(0, "B", 1).set(2, "A", 1).set(3, "C", 1).set(4, "D", 1);
        ReorderState<String> rs = new ReorderState<>(m, byName());
        SwapOp s;
        while ((s = rs.next()) != null) {
            assertNotEquals(1, s.a());
            assertNotEquals(1, s.b());
            applySwap(m, s);
        }
        assertEquals("Z", m.items[1]);
        assertEquals(99, m.counts[1]);
        // 参与槽按 A,B,C,D 排列
        assertEquals("A", m.items[0]);
        assertEquals("B", m.items[2]);
        assertEquals("C", m.items[3]);
        assertEquals("D", m.items[4]);
    }

    @Test
    void reorderByCountDesc() {
        Model m = new Model(4, 64, 0)
                .set(0, "X", 5).set(1, "Y", 50).set(2, "X", 30).set(3, "Y", 3);
        OrderCmp<String> byCountDesc = (a, ac, b, bc) -> Long.compare(bc, ac); // 数量大者在前
        ReorderState<String> rs = new ReorderState<>(m, byCountDesc);
        SwapOp s;
        int guard = 0;
        while ((s = rs.next()) != null && guard++ < 20) applySwap(m, s);
        // 数量降序：Y50, X30, X5, Y3 —— 同数量档位间保持稳定（X5 在 Y3 之前由原序决定）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (m.items[i] != null) sb.append(m.items[i]).append(m.counts[i]).append(' ');
        }
        assertEquals("Y50 X30 X5 Y3 ", sb.toString());
    }

    @Test
    void mergeThenReorderEndsFullySorted() {
        Model m = new Model(8, 64, 0)
                .set(0, "D", 20).set(1, "A", 10).set(2, "A", 10)
                .set(3, "C", 15).set(4, "A", 12).set(5, "B", 5).set(6, "B", 2).set(7, "D", 30);
        int guard = 0;
        MergeOp mo;
        while ((mo = SortEngine.findMerge(m)) != null && guard++ < 200) applyMerge(m, mo);
        ReorderState<String> rs = new ReorderState<>(m, byName());
        guard = 0;
        SwapOp so;
        while ((so = rs.next()) != null && guard++ < 100) applySwap(m, so);
        String[] items = new String[8];
        for (int i = 0; i < 8; i++) items[i] = m.items[i] == null ? "" : m.items[i];
        // A(32 堆成两满槽+? ) 合并结果应类似：A,A(满)? 以容量 64 计：A 总 32 → 单槽; 但合并会把 A 两堆并到槽1=32... 排序最终期望 A、B、C、D 各就位
        List<String> prefix = new ArrayList<>();
        for (String it : items) if (!it.isEmpty()) prefix.add(it);
        // 前缀应为升序（可能含重复键相邻）
        for (int i = 1; i < prefix.size(); i++) {
            assertTrue(prefix.get(i - 1).compareTo(prefix.get(i)) <= 0, "未完全升序: " + prefix);
        }
        assertEquals("", items[7]); // 应有空槽
    }
}
