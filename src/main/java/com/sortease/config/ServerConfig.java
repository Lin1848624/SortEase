package com.sortease.config;

import com.sortease.SortedMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 服务端配置 sortease-server.toml（整理引擎预算等权威项）。 */
@EventBusSubscriber(modid = SortedMod.MODID)
public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_OPS_PER_TICK = BUILDER
            .comment("整理引擎每个服务端 tick 最多执行的槽位原子操作数（防大容器卡顿）")
            .defineInRange("maxOpsPerTick", 128, 8, 4096);
    public static final ModConfigSpec.IntValue MAX_JOB_TICKS = BUILDER
            .comment("单个整理任务允许跨越的最大 tick 数（防异常死循环）")
            .defineInRange("maxJobTicks", 1200, 50, 60000);
    public static final ModConfigSpec.IntValue HARD_MAX_OPS = BUILDER
            .comment("单个整理任务累计原子操作硬上限（安全阀）")
            .defineInRange("hardMaxOps", 40000, 1000, 500000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int maxOpsPerTick;
    public static int maxJobTicks;
    public static int hardMaxOps;

    private ServerConfig() {}

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        refresh();
    }

    public static void refresh() {
        maxOpsPerTick = MAX_OPS_PER_TICK.get();
        maxJobTicks = MAX_JOB_TICKS.get();
        hardMaxOps = HARD_MAX_OPS.get();
    }
}
