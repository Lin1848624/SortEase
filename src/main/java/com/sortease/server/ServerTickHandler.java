package com.sortease.server;

import com.sortease.SortedMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 服务端 tick 驱动整理任务（在单机集成服务端同样生效）。 */
@EventBusSubscriber(modid = SortedMod.MODID)
public final class ServerTickHandler {
    private ServerTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        try {
            SortJobManager.tickServer(event.getServer());
        } catch (RuntimeException ex) {
            com.sortease.SortedMod.LOGGER.error("Sorted sort tick failed", ex);
        }
    }
}
