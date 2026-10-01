package com.sortease.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** 客户端专属注册（配置界面入口等）。仅在物理客户端调用，专用服务端不会加载本类。 */
public final class ClientReg {
    private ClientReg() {}

    public static void initConfigScreen(ModContainer container) {
        // 1.21.1 的扩展点方法签名为 IConfigScreenFactory#createScreen(ModContainer, Screen)
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new com.sortease.client.gui.SortedConfigScreen(parent));
    }
}
