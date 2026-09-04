package com.sortease.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** 客户端专属注册（配置界面入口等）。 */
public final class ClientReg {
    private ClientReg() {}

    public static void initConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new com.sortease.client.gui.SortedConfigScreen(parent)));
    }
}
