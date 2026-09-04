package com.sortease.networking;

import com.sortease.SortedMod;
import com.sortease.networking.msg.SortAckMessage;
import com.sortease.networking.msg.SortRequestMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Networking {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SortedMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private Networking() {}

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, SortRequestMessage.class,
                SortRequestMessage::encode, SortRequestMessage::decode,
                SortRequestMessage::handle);
        CHANNEL.messageBuilder(SortAckMessage.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SortAckMessage::encode)
                .decoder(SortAckMessage::decode)
                .consumerMainThread(SortAckMessage::handle)
                .add();
    }
}
