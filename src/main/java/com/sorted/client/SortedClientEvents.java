package com.sorted.client;

import com.sorted.classify.ContainerClassifier;
import com.sorted.classify.MenuProfile;
import com.sorted.config.ClientConfig;
import com.sorted.networking.Networking;
import com.sorted.networking.msg.SortRequestMessage;
import com.sorted.sort.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** 客户端事件接线：覆盖层渲染/交互、整理快捷键、设置界面。 */
@Mod.EventBusSubscriber(modid = "sorted", value = Dist.CLIENT)
public final class SortedClientEvents {
    private static long lastSortMs;

    private SortedClientEvents() {}

    private static boolean pressed(int keyCode, net.minecraft.client.KeyMapping mapping) {
        return mapping.getKey().getValue() == keyCode;
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> scr) {
            OverlayController.refresh(scr);
            if (ClientConfig.overlayDefaultOn) OverlayController.setEnabled(true);
        } else {
            OverlayController.refresh(null);
            if (!(event.getScreen() instanceof com.sorted.client.gui.SortedConfigScreen)) {
                OverlayController.setEnabled(false);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        OverlayController.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onMouse(ScreenEvent.MouseButtonPressed event) {
        if (OverlayController.isEnabled()
                && OverlayController.handleClick(event.getButton(), event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed event) {
        if (event.getScreen().getFocused() instanceof EditBox) return;
        int kc = event.getKeyCode();
        Minecraft mc = Minecraft.getInstance();

        if (pressed(kc, KeyBindings.OVERLAY)) {
            event.setCanceled(true);
            if (event.getScreen() instanceof AbstractContainerScreen<?> scr) {
                OverlayController.setEnabled(!OverlayController.isEnabled());
                OverlayController.refresh(scr);
            }
            return;
        }
        if (pressed(kc, KeyBindings.CONFIG)) {
            event.setCanceled(true);
            mc.setScreen(new com.sorted.client.gui.SortedConfigScreen(event.getScreen()));
            return;
        }
        if (pressed(kc, KeyBindings.SORT)) {
            event.setCanceled(true);
            requestSort();
        }
    }

    /** 由当前打开容器构建并发送整理请求。 */
    private static void requestSort() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.screen instanceof AbstractContainerScreen<?> scr)) return;
        long now = System.currentTimeMillis();
        if (now - lastSortMs < 400) return;
        lastSortMs = now;

        MenuProfile profile = ContainerClassifier.classify(scr.getMenu(), mc.player.getInventory());
        if (!profile.canSort()) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("sorted.ack.unsupported"), true);
            return;
        }

        List<Integer> forceSort = new ArrayList<>();
        List<Integer> forceIgnore = new ArrayList<>();
        int[] ovr = OverrideStore.allOf(profile.menuKey, profile.slotCount());
        for (int i = 0; i < profile.slotCount(); i++) {
            if (i >= ovr.length) break;
            if (ovr[i] == 1 && !profile.sortableByDefault(i)) forceSort.add(i);
            if (ovr[i] == 2 && profile.sortableByDefault(i)) forceIgnore.add(i);
        }

        int mode = Math.max(0, Math.min(SortMode.values().length - 1, ClientConfig.sortMode));
        int containerId = scr.getMenu().containerId;
        Networking.CHANNEL.sendToServer(new SortRequestMessage(
                containerId, mode, ClientConfig.sortAscending,
                ClientConfig.includePlayerMain, ClientConfig.includeHotbar,
                forceSort.stream().mapToInt(Integer::intValue).toArray(),
                forceIgnore.stream().mapToInt(Integer::intValue).toArray()));
    }
}
