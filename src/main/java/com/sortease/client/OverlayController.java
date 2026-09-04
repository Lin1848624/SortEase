package com.sortease.client;

import com.sortease.classify.ContainerClassifier;
import com.sortease.classify.MenuProfile;
import com.sortease.classify.SlotKind;
import com.sortease.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** 槽位分类覆盖层的渲染与点选逻辑（坐标来自 OverlayGuiPos，不直接碰屏幕受保护字段）。 */
public final class OverlayController {
    private static boolean enabled;
    private static AbstractContainerScreen<?> screen;
    private static MenuProfile profile;
    private static int[] overrides;

    private OverlayController() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void refresh(AbstractContainerScreen<?> scr) {
        screen = scr;
        profile = null;
        overrides = null;
        if (scr == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        profile = ContainerClassifier.classify(scr.getMenu(), mc.player.getInventory());
        overrides = OverrideStore.allOf(profile.menuKey, profile.slotCount());
    }

    public static void setEnabled(boolean flag) {
        enabled = flag;
        if (flag && screen == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof AbstractContainerScreen<?> s) refresh(s);
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
            OverrideStore.resetMenu(profile.menuKey);
        } else if (button == 0) {
            OverrideStore.cycle(profile.menuKey, idx, profile.slotCount());
        } else {
            return false;
        }
        overrides = OverrideStore.allOf(profile.menuKey, profile.slotCount());
        return true;
    }

    private static int baseColor(SlotKind kind) {
        return switch (kind) {
            case MAIN -> 0xFF2ECC71;      // 绿：主体存储
            case PLAYER_MAIN -> 0xFF3498DB; // 蓝：玩家主栏
            case PLAYER_HOTBAR -> 0xFF1ABC9C; // 青：快捷栏
            default -> 0xFFE74C3C;        // 红：特殊/忽略
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
            int x0 = OverlayGuiPos.left() + s.x;
            int y0 = OverlayGuiPos.top() + s.y;
            int ov = i < overrides.length ? overrides[i] : 0;
            SlotKind kind = profile.kind(i);
            int color = (baseColor(kind) & 0x00FFFFFF) | a;
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
