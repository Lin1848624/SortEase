package com.sortease;

import com.mojang.logging.LogUtils;
import com.sortease.client.ClientReg;
import com.sortease.config.ClientConfig;
import com.sortease.config.ServerConfig;
import com.sortease.networking.Networking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SortedMod.MODID)
public class SortedMod {
    public static final String MODID = "sortease";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SortedMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modEventBus.register(ClientConfig.class);
        modEventBus.register(ServerConfig.class);

        Networking.init();

        // 让 Mods 列表的「配置」按钮打开 Sorted 设置界面（仅客户端注册，避免专用服务端加载客户端类）
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientReg::initConfigScreen);
    }
}
