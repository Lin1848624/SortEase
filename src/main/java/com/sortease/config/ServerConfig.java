package com.sortease.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** 服务端配置 sorted-server.toml（整理引擎预算等权威项，客户端只需与之同步）。 */
@Mod.EventBusSubscriber(modid = "sortease", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_OPS_PER_TICK = BUILDER
            .comment("整理引擎每个服务端 tick 最多执行的槽位原子操作数（防大容器卡顿）")
            .defineInRange("maxOpsPerTick", 128, 8, 4096);
    public static final ForgeConfigSpec.IntValue MAX_JOB_TICKS = BUILDER
            .comment("单个整理任务允许跨越的最大 tick 数（防异常死循环）")
            .defineInRange("maxJobTicks", 1200, 50, 60000);
    public static final ForgeConfigSpec.IntValue HARD_MAX_OPS = BUILDER
            .comment("单个整理任务累计原子操作硬上限（安全阀）")
            .defineInRange("hardMaxOps", 40000, 1000, 500000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int maxOpsPerTick;
    public static int maxJobTicks;
    public static int hardMaxOps;

    private ServerConfig() {}

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        refresh();
    }

    public static void refresh() {
        maxOpsPerTick = MAX_OPS_PER_TICK.get();
        maxJobTicks = MAX_JOB_TICKS.get();
        hardMaxOps = HARD_MAX_OPS.get();
    }
}
