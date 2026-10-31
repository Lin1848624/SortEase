package com.sortease.client;

import com.sortease.classify.ContainerClassifier;
import com.sortease.classify.MenuProfile;
import com.sortease.classify.SlotKind;
import com.sortease.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** 槽位分类覆盖层的渲染与点选逻辑（坐标来自 OverlayGuiPos，不直接碰屏幕受保护字段）。 */
public final class OverlayController {
    private static boolean enabled;
    private static AbstractContainerScreen<?> screen;
    private static MenuProfile profile;
    private static int[] overrides;
    /** 上次分类时菜单“槽位可用性”指纹：深口袋附魔等级变化会改变它。 */
    private static long availabilitySig;

    private OverlayController() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void refresh(AbstractContainerScreen<?> scr) {
        screen = scr;
        profile = null;
        overrides = null;
        availabilitySig = 0L;
        if (scr == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        profile = ContainerClassifier.classify(scr.getMenu(), mc.player.getInventory());
        overrides = OverrideStore.allOf(profile.menuKey, profile.slotCount());
        availabilitySig = availabilityOf(scr.getMenu());
    }

    /**
     * 深口袋等级变化（换护腿）会让可用格数改变；发现槽位可用性变化时重新分类一次，
     * 避免覆盖层继续用旧的“哪些格已解锁”结果。
     */
    public static void refreshIfStale() {
        if (!enabled || screen == null || profile == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != screen) return; // 屏幕已经切走，等 ScreenEvent.Init 再刷新
        long sig = availabilityOf(screen.getMenu());
        if (sig != availabilitySig) {
            refresh(screen);
        }
    }

    private static long availabilityOf(AbstractContainerMenu menu) {
        long h = menu.slots.size();
        for (Slot s : menu.slots) {
            h = h * 31 + (ContainerClassifier.isActive(s) ? 1 : 0);
        }
        return h;
    }

    public static void setEnabled(boolean flag) {
        enabled = flag;
        if (!flag) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> current)) return;
        if (screen != current || profile == null) {
            refresh(current);
        } else {
            // 覆盖层关闭期间附魔等级可能变过，重新分类一次保证颜色正确。
            refreshIfStale();
        }
    }

    public static MenuProfile profile() {
        return profile;
    }

    private static boolean guiPosValid() {
        return OverlayGuiPos.valid(screen);
    }

    private static int slotIndexAt(double mx, double my) {
        if (screen == null || !guiPosValid()) return -1;
        var slots = screen.getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            // 未激活的槽位不在界面上（如 Inventorio 未解锁的深口袋行），不参与点选。
            if (!ContainerClassifier.isActive(s)) continue;
            int x0 = OverlayGuiPos.left() + s.x;
            int y0 = OverlayGuiPos.top() + s.y;
            if (mx >= x0 && mx < x0 + 16 && my >= y0 && my < y0 + 16) {
                return i;
            }
        }
        return -1;
    }

    /** 处理一次覆盖层内的点击；返回 true 表示已消费。 */
    public static boolean handleClick(int button, double mx, double my) {
        if (!enabled || profile == null) return false;
        int idx = slotIndexAt(mx, my);
        if (idx < 0) return false;
        if (button == 1) {
            // 右键：恢复本容器的全部“自动”分类（锁定槽也允许触发）。
            OverrideStore.resetMenu(profile.menuKey);
        } else if (button == 0) {
            if (profile.hardLocked(idx)) {
                // 永久锁定槽（快捷腰带/工具腰带/未解锁深口袋/本界面的快捷栏）：
                // 吃掉这次点击避免误拖物品，但不再切换分类。
                return true;
            }
            OverrideStore.cycle(profile.menuKey, idx, profile.slotCount());
        } else {
            return false;
        }
        overrides = OverrideStore.allOf(profile.menuKey, profile.slotCount());
        return true;
    }

    private static int baseColor(SlotKind kind) {
        return switch (kind) {
            case MAIN -> 0xFF2ECC71;          // 绿：主体存储
            case PLAYER_MAIN -> 0xFF3498DB;   // 蓝：玩家主栏
            case PLAYER_HOTBAR -> 0xFF1ABC9C; // 青：快捷栏
            case DEEP_POCKETS -> 0xFF9B59B6;  // 紫：深口袋（附魔）
            default -> 0xFFE74C3C;            // 红：特殊/忽略
        };
    }

    public static void render(GuiGraphics g) {
        if (!enabled || profile == null || screen == null) return;
        // 未捕获到坐标时静默跳过（避免在异常/第三方子类屏幕上崩溃）
        if (!guiPosValid()) return;
        Minecraft mc = Minecraft.getInstance();
        int alpha = Math.max(0, Math.min(100, ClientConfig.overlayOpacity));
        int a = alpha << 24;
        for (int i = 0; i < screen.getMenu().slots.size(); i++) {
            Slot s = screen.getMenu().slots.get(i);
            // 未激活的槽位不绘制，避免在界面下方空白处画出不存在的格子（Inventorio 深口袋）。
            if (!ContainerClassifier.isActive(s)) continue;
            int x0 = OverlayGuiPos.left() + s.x;
            int y0 = OverlayGuiPos.top() + s.y;
            int ov = i < overrides.length ? overrides[i] : 0;
            SlotKind kind = profile.kind(i);
            int color = (baseColor(kind) & 0x00FFFFFF) | a;
            if (profile.hardLocked(i)) {
                // 锁定（不会参与整理）：先压暗，再画分类颜色描边，仍能看清它是什么。
                g.fill(x0, y0, x0 + 16, y0 + 16, 0x55000000);
            }
            drawBorder(g, x0, y0, 16, 16, color);
            if (ov == 1) {
                g.fill(x0 + 5, y0 + 5, x0 + 11, y0 + 11, 0xFFFFFFFF);
            } else if (ov == 2) {
                g.fill(x0, y0, x0 + 16, y0 + 16, 0x44000000);
            }
        }
        String hint = net.minecraft.network.chat.Component.translatable("sorted.overlay.hint").getString();
        g.drawCenteredString(mc.font, hint, screen.width / 2, 4, 0xFFFFFF);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
