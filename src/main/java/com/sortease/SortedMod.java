package com.sortease;

import com.mojang.logging.LogUtils;
import com.sortease.client.ClientReg;
import com.sortease.config.ClientConfig;
import com.sortease.config.ServerConfig;
import com.sortease.networking.Networking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(SortedMod.MODID)
public class SortedMod {
    public static final String MODID = "sortease";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SortedMod(IEventBus modEventBus, ModContainer modContainer) {
        // 配置文件：config/sortease-client.toml（客户端偏好）与 config/sortease-server.toml（服务端预算）
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        // 注册自定义网络载荷（1.21 的 CustomPacketPayload 通道）
        Networking.init(modEventBus);

        // 让 Mods 列表的「配置」按钮打开 SortEase 设置界面（仅客户端注册，避免专用服务端加载客户端类）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientReg.initConfigScreen(modContainer);
        }
    }
}
