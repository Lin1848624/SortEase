package com.sortease.client;

import com.sortease.classify.ContainerClassifier;
import com.sortease.classify.MenuProfile;
import com.sortease.config.ClientConfig;
import com.sortease.networking.Networking;
import com.sortease.networking.msg.SortRequestMessage;
import com.sortease.sort.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** 客户端事件接线：覆盖层渲染/交互、整理快捷键、设置界面。 */
@Mod.EventBusSubscriber(modid = "sortease", value = Dist.CLIENT)
public final class SortedClientEvents {
    private static long lastSortMs;

    /** 视觉去重：上次整理完成后客户端界面内容签名；内容未变化时按键直接忽略。 */
    private static long lastViewSig;
    private static boolean hasViewBaseline;
    private static int pendingViewCapture;

    private SortedClientEvents() {}

    /** 客户端侧对“玩家看到的菜单内容”做签名（与服务端签名同构，用于判断界面是否变了）。 */
    private static long viewSigOf(AbstractContainerMenu menu) {
        long h = 0x9E3779B97F4A7C15L;
        for (Slot s : menu.slots) {
            long v = 0;
            ItemStack it = s.getItem();
            if (!it.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(it.getItem());
                String name = id == null ? "<unknown>" : id.toString();
                v = name.hashCode();
                v = v * 31 + it.getCount();
                long tagH = it.getTag() == null ? 0 : it.getTag().hashCode();
                v = v * 31 + tagH;
            }
            h ^= v;
            h *= 0x100000001B3L;
        }
        return h;
    }

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
            if (!(event.getScreen() instanceof com.sortease.client.gui.SortedConfigScreen)) {
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
            mc.setScreen(new com.sortease.client.gui.SortedConfigScreen(event.getScreen()));
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

        // 视觉去重：界面显示的内容与上次整理完成时完全一致 → 无需再发请求（避免无意义挪动）。
        long curSig = viewSigOf(scr.getMenu());
        if (hasViewBaseline && curSig == lastViewSig) return;

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

    /** 收到服务端“整理完成/无需整理”回执后调用：等槽位同步落定再记录基准，供视觉去重使用。 */
    public static void scheduleViewCapture() {
        pendingViewCapture = 2;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingViewCapture <= 0) return;
        if (--pendingViewCapture == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen instanceof AbstractContainerScreen<?> scr) {
                lastViewSig = viewSigOf(scr.getMenu());
                hasViewBaseline = true;
            }
        }
    }
}
