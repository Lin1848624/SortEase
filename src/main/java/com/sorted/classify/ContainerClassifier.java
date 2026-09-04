package com.sorted.classify;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.IdentityHashMap;
import java.util.Locale;

/**
 * 槽位分类器。结果必须「客户端/服务端一致」：只依据两侧镜像结构
 * （菜单类名、IItemHandler/容器对象身份、槽内序号、容量、槽位子类名）。
 *
 * 规则层级：
 * 1) 玩家主栏/快捷栏/护甲副手（依据容器对象 + 槽内序号）；
 * 2) 合成区/结果区容器 → 特殊；
 * 3) 容量最大的非玩家存储 → 主体(MAIN)，其余较小存储 → 特殊；
 * 4) 通用后处理：非原版 Slot 子类名含 upgrade/filter/tool/void 等风险词 → 特殊
 *    （用于 Create:Storage 的 ToolSlot/UpgradeSlot/VoidSlot、精妙背包的 BackpackUpgradeSlot 等）；
 * 5) 预设(Preset)可再调整。
 */
public final class ContainerClassifier {
    private static final int MIN_MAIN_SIZE = 4;

    private static final String[] RISKY_SLOT_NAMES = {
            "upgrade", "filter", "tool", "jukebox", "disc", "void", "ghost", "tank", "charge"
    };

    public interface Preset {
        String id();

        boolean matches(AbstractContainerMenu menu);

        void apply(MenuProfile profile, AbstractContainerMenu menu);
    }

    private static final java.util.List<Preset> PRESETS = new java.util.ArrayList<>();

    private ContainerClassifier() {}

    public static void addPreset(Preset preset) {
        PRESETS.add(preset);
    }

    public static Object storageKeyOf(Slot slot) {
        if (slot instanceof SlotItemHandler sh) return sh.getItemHandler();
        Object c = slot.container;
        return c != null ? c : slot;
    }

    private static int storageSize(Object key) {
        try {
            if (key instanceof IItemHandler h) return h.getSlots();
            if (key instanceof Container c) return c.getContainerSize();
        } catch (RuntimeException ignored) {
        }
        return 0;
    }

    private static boolean isRiskSlotClass(Slot slot) {
        // 仅排除“纯原版”槽位类型；任何自定义子类都按其类名判定（含 SlotItemHandler 的子类）
        String cls = slot.getClass().getName();
        if (cls.equals(Slot.class.getName())) return false;
        if (cls.equals(SlotItemHandler.class.getName())) return false;
        String simple = slot.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        for (String kw : RISKY_SLOT_NAMES) {
            if (simple.contains(kw)) return true;
        }
        return false;
    }

    public static MenuProfile classify(AbstractContainerMenu menu, Container playerInv) {
        int n = menu.slots.size();
        boolean inventoryScreen = menu instanceof InventoryMenu;
        MenuProfile profile = new MenuProfile(menu.getClass().getName(), n, inventoryScreen, false, false);

        Object[] keyOfSlot = new Object[n];
        for (int i = 0; i < n; i++) {
            Slot s = menu.getSlot(i);
            if (s instanceof SlotItemHandler) {
                keyOfSlot[i] = storageKeyOf(s);
                continue;
            }
            Container c = s.container;
            if (c == null) {
                continue; // 保持默认 SPECIAL
            }
            if (c == playerInv) {
                int cs = s.getContainerSlot();
                if (cs >= 0 && cs <= 8) {
                    profile.setKind(i, SlotKind.PLAYER_HOTBAR);
                } else if (cs <= 35) {
                    profile.setKind(i, SlotKind.PLAYER_MAIN);
                }
                // 护甲/副手等保持 SPECIAL
            } else if (c instanceof CraftingContainer || c instanceof ResultContainer) {
                // 合成/结果区保持 SPECIAL
            } else {
                keyOfSlot[i] = c;
            }
        }

        // 选出容量最大的主体存储键
        Object mainKey = null;
        int best = -1;
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        for (Object key : keyOfSlot) {
            if (key == null || seen.containsKey(key)) continue;
            seen.put(key, Boolean.TRUE);
            int sz = storageSize(key);
            if (sz > MIN_MAIN_SIZE && sz > best) {
                best = sz;
                mainKey = key;
            }
        }

        for (int i = 0; i < n; i++) {
            if (keyOfSlot[i] == mainKey) {
                profile.setKind(i, SlotKind.MAIN);
            }
        }

        // 通用风险槽后处理：主体存储内的自定义槽位若是升级/过滤/工具等 → 改回特殊
        for (int i = 0; i < n; i++) {
            if (profile.kind(i) == SlotKind.MAIN && isRiskSlotClass(menu.getSlot(i))) {
                profile.setKind(i, SlotKind.SPECIAL);
            }
        }

        for (Preset p : PRESETS) {
            if (p.matches(menu)) p.apply(profile, menu);
        }

        // 重算是否存在可整理主体（预设可能把全部降级成特殊）
        boolean hasMain = false;
        for (int i = 0; i < n; i++) {
            if (profile.kind(i) == SlotKind.MAIN) {
                hasMain = true;
                break;
            }
        }
        profile.hasMainContainer = hasMain;
        return profile;
    }
}
