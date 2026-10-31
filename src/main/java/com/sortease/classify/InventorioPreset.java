package com.sortease.classify;

import com.sortease.SortedMod;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Inventorio 兼容预设。
 *
 * 玩家自己的 Inventorio 物品栏界面（{@code de.rubixdev.inventorio.player.InventorioScreenHandler}）里：
 * <ul>
 *   <li>深口袋（Deep Pockets 附魔，每级解锁一排 9 格，最多 27 格）是玩家背包的延伸 → DEEP_POCKETS，参与整理；</li>
 *   <li>快捷腰带（附加容器第 27~34 格）与工具腰带（第 35 格起）是 Inventorio 的快捷装备区 → 永久锁定；</li>
 *   <li>尚未解锁的深口袋格 → 永久锁定；</li>
 *   <li>快捷栏在本界面内不参与整理（用户要求），同样永久锁定。</li>
 * </ul>
 *
 * 只按菜单类名 / 槽位类名 / 容器序号识别，不编译期依赖 Inventorio。
 * 未安装 Inventorio、或将来它改了类名时本预设不会命中，退回通用规则（不会崩溃）。
 */
public final class InventorioPreset implements ContainerClassifier.Preset {
    /** Inventorio 物品栏菜单类名（1.20.1 Forge v1.10.x 与 1.21.1 NeoForge v1.11.x 一致）。 */
    public static final String MENU_CLASS = "de.rubixdev.inventorio.player.InventorioScreenHandler";
    /** 玩家附加容器（深口袋 + 快捷腰带 + 工具腰带）的运行时类名。 */
    private static final String ADDON_CLASS = "de.rubixdev.inventorio.player.PlayerInventoryAddon";
    /** Inventorio 自定义槽位所在包（DeepPocketsSlot / ToolBeltSlot / ArmorSlot / BlockedSlot）。 */
    private static final String SLOT_PACKAGE_PREFIX = "de.rubixdev.inventorio.slot.";

    /** 深口袋总格数：3 级 × 9 格。 */
    private static final int DEEP_POCKETS_SIZE = 27;
    /** 附加容器序号：深口袋 0..26、快捷腰带 27..34、工具腰带 35 起。 */
    private static final int UTILITY_BELT_END = 35;

    private static boolean logged;

    @Override
    public String id() {
        return "inventorio";
    }

    @Override
    public boolean matches(AbstractContainerMenu menu) {
        if (menu == null) return false;
        // 沿继承链匹配：Curios 等集成可能产生子类，但运行时类名始终以 Inventorio 的处理器为父类。
        Class<?> c = menu.getClass();
        for (int depth = 0; c != null && depth < 8; depth++, c = c.getSuperclass()) {
            if (MENU_CLASS.equals(c.getName())) return true;
        }
        return false;
    }

    @Override
    public void apply(MenuProfile profile, AbstractContainerMenu menu) {
        // 这是玩家自己的背包界面：即使关闭“整理玩家主栏”，主栏也应可整理（与打开原版物品栏一致）。
        profile.setInventoryScreen(true);

        int activeDeep = 0;
        int lockedDeep = 0;
        int utility = 0;
        int toolBelt = 0;
        int hotbarLocked = 0;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);
            Container c = slot.container;
            if (c instanceof Inventory) {
                // 玩家物品栏：主栏沿用通用分类（PLAYER_MAIN）。
                // 本界面内快捷栏不参与整理；护甲/副手仍由既有硬隔离兜底。
                int cs = slot.getContainerSlot();
                if (cs >= 0 && cs <= 8) {
                    profile.setHardLocked(i);
                    hotbarLocked++;
                }
                continue;
            }
            if (!isAddonSlot(slot, c)) {
                // 合成格 / 结果格 / 其它非玩家容器：保持特殊并硬隔离。
                profile.setKind(i, SlotKind.SPECIAL);
                profile.setHardLocked(i);
                continue;
            }
            int idx = slot.getContainerSlot();
            if (idx >= 0 && idx < DEEP_POCKETS_SIZE && ContainerClassifier.isActive(slot)) {
                profile.setKind(i, SlotKind.DEEP_POCKETS);
                activeDeep++;
            } else {
                profile.setKind(i, SlotKind.SPECIAL);
                profile.setHardLocked(i);
                if (idx >= 0 && idx < DEEP_POCKETS_SIZE) {
                    lockedDeep++;
                } else if (idx < UTILITY_BELT_END) {
                    utility++;
                } else {
                    toolBelt++;
                }
            }
        }

        if (!logged) {
            logged = true;
            SortedMod.LOGGER.info(
                    "[Sorted] Inventorio screen detected: deepPocketsActive={} deepPocketsLocked={} utility={} toolBelt={} hotbarLocked={}",
                    activeDeep, lockedDeep, utility, toolBelt, hotbarLocked);
        }
    }

    private static boolean isAddonSlot(Slot slot, Container c) {
        if (c != null && ADDON_CLASS.equals(c.getClass().getName())) {
            return true;
        }
        // 兜底：Inventorio 自定义槽位类型（只会在非玩家 Inventory 容器上调用到这里）。
        return slot.getClass().getName().startsWith(SLOT_PACKAGE_PREFIX);
    }
}
