package com.sortease.client.gui;

import com.sortease.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** Sorted 设置界面：改动即时写盘，另提供恢复默认。 */
public class SortedConfigScreen extends Screen {
    private static final String[] MODE_KEYS = {"sort.mode.merge", "sort.mode.name", "sort.mode.id", "sort.mode.mod", "sort.mode.count"};

    private final Screen parent;

    public SortedConfigScreen(Screen parent) {
        super(Component.translatable("gui.sorted.config_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 150;
        int right = this.width / 2 + 150;
        int yTop = 16;
        int bottomY = this.height - 28;
        int spacing = Math.max(18, Math.min(26, (bottomY - yTop - 24) / 8));
        int labelX = left + 2;
        int btnW = 128;
        int btnX = right - btnW;
        int y = yTop;

        List<RowDef> defs = new ArrayList<>();
        defs.add(cycleRow("gui.sorted.mode", currentModeText(),
                () -> ClientConfig.SORT_MODE.set((ClientConfig.SORT_MODE.get() + 1) % 5)));
        defs.add(boolRow("gui.sorted.ascending", ClientConfig.SORT_ASCENDING));
        defs.add(boolRow("gui.sorted.include_main", ClientConfig.INCLUDE_PLAYER_MAIN));
        defs.add(boolRow("gui.sorted.include_hotbar", ClientConfig.INCLUDE_HOTBAR));
        defs.add(boolRow("gui.sorted.unknown_sortable", ClientConfig.UNKNOWN_AS_SORTABLE));
        defs.add(boolRow("gui.sorted.overlay_default", ClientConfig.OVERLAY_DEFAULT_ON));
        defs.add(cycleRow("gui.sorted.overlay_opacity", Component.literal(String.valueOf(ClientConfig.OVERLAY_OPACITY.get())),
                () -> ClientConfig.OVERLAY_OPACITY.set((ClientConfig.OVERLAY_OPACITY.get() + 5) % 105)));

        for (RowDef def : defs) {
            final int rowY = y;
            Button btn = def.factory().make(btnX, rowY, btnW, 18);
            this.addRenderableWidget(btn);
            this.addRenderableWidget(new LabelWidget(Component.translatable(def.labelKey), labelX, rowY));
            y += spacing;
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.sorted.reset"), b -> {
            ClientConfig.SORT_MODE.set(0);
            ClientConfig.SORT_ASCENDING.set(true);
            ClientConfig.INCLUDE_PLAYER_MAIN.set(true);
            ClientConfig.INCLUDE_HOTBAR.set(true);
            ClientConfig.UNKNOWN_AS_SORTABLE.set(true);
            ClientConfig.OVERLAY_DEFAULT_ON.set(false);
            ClientConfig.OVERLAY_OPACITY.set(55);
            refreshAndSave();
            this.minecraft.setScreen(new SortedConfigScreen(parent));
        }).bounds(left, bottomY, 96, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.sorted.close"), b -> onClose())
                .bounds(right - 96, bottomY, 96, 20).build());
    }

    private RowDef boolRow(String labelKey, ForgeConfigSpec.BooleanValue value) {
        return new RowDef(labelKey, (x, y, w, h) -> Button.builder(currentBoolText(value), b -> {
            value.set(!value.get());
            refreshAndSave();
            b.setMessage(currentBoolText(value));
        }).bounds(x, y, w, h).build());
    }

    private RowDef cycleRow(String labelKey, Component initial, Runnable action) {
        return new RowDef(labelKey, (x, y, w, h) -> Button.builder(initial, b -> {
            action.run();
            refreshAndSave();
            this.minecraft.setScreen(new SortedConfigScreen(parent));
        }).bounds(x, y, w, h).build());
    }

    private void refreshAndSave() {
        ClientConfig.refresh();
        ClientConfig.SPEC.save();
    }

    private Component currentModeText() {
        return Component.translatable(MODE_KEYS[ClientConfig.SORT_MODE.get()]);
    }

    private static Component currentBoolText(ForgeConfigSpec.BooleanValue value) {
        return Component.translatable(value.get() ? "gui.sorted.on" : "gui.sorted.off");
    }

    @FunctionalInterface
    private interface ButtonFactory {
        Button make(int x, int y, int w, int h);
    }

    private record RowDef(String labelKey, ButtonFactory factory) {
    }

    private final class LabelWidget extends AbstractWidget {
        LabelWidget(Component message, int x, int y) {
            super(x, y, 100, 18, message);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, getMessage(), getX(), getY() + 5, 0xFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);
        super.render(g, mx, my, partialTick);
    }

    @Override
    public void onClose() {
        refreshAndSave();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
