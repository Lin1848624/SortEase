package com.sortease.server;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 服务端 tick 驱动整理任务（在单机集成服务端同样生效）。 */
@Mod.EventBusSubscriber(modid = "sortease")
public final class ServerTickHandler {
    private ServerTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!SortJobManager.hasJobs()) return;
        try {
            SortJobManager.tickServer(event.getServer());
        } catch (RuntimeException ex) {
            com.sortease.SortedMod.LOGGER.error("Sorted sort tick failed", ex);
        }
    }
}
