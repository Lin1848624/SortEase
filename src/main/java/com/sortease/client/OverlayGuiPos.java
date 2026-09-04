package com.sortease.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** 保存最近一次屏幕渲染时的 GUI 原点（由 AbstractContainerScreen 渲染 Mixin 写入）。 */
public final class OverlayGuiPos {
    private static AbstractContainerScreen<?> screen;
    private static int left;
    private static int top;

    private OverlayGuiPos() {}

    public static void capture(AbstractContainerScreen<?> scr, int l, int t) {
        screen = scr;
        left = l;
        top = t;
    }

    public static boolean valid(AbstractContainerScreen<?> scr) {
        return scr != null && screen == scr;
    }

    public static int left() {
        return left;
    }

    public static int top() {
        return top;
    }
}
