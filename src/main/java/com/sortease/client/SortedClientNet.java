package com.sortease.client;

import com.sortease.networking.msg.SortAckMessage;
import net.minecraft.client.Minecraft;

/** 客户端处理整理回执：成功的“已整理/无需整理”不打扰玩家，仅保留异常类提示。 */
public final class SortedClientNet {
    private SortedClientNet() {}

    public static void onAck(SortAckMessage msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // 正常完成/无变化：静默，不弹 ActionBar
        if (msg.code == SortAckMessage.OK || msg.code == SortAckMessage.NO_CHANGE) return;

        String key = msg.messageKey;
        if (key == null || key.isEmpty()) {
            key = switch (msg.code) {
                case SortAckMessage.CANCELLED -> "sorted.ack.cancelled";
                case SortAckMessage.UNSUPPORTED -> "sorted.ack.unsupported";
                case SortAckMessage.BUSY -> "sorted.ack.busy";
                default -> "sorted.ack.cancelled";
            };
        }
        mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
    }
}
