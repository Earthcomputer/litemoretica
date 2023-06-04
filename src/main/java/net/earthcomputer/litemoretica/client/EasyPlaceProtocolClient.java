package net.earthcomputer.litemoretica.client;

import com.google.common.collect.ImmutableSet;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.PlacementHandler;
import io.netty.buffer.Unpooled;
import net.earthcomputer.litemoretica.mixin.client.PlacementHandlerAccessor;
import net.earthcomputer.litemoretica.network.InitEasyPlaceProtocolPacket;
import net.earthcomputer.litemoretica.network.SetEasyPlaceProtocolPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.state.property.Property;

public final class EasyPlaceProtocolClient {
    private static final ImmutableSet<Property<?>> DEFAULT_WHITELISTED_PROPERTIES = PlacementHandler.WHITELISTED_PROPERTIES;
    public static boolean serverHasV3Protocol = false;

    private EasyPlaceProtocolClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(InitEasyPlaceProtocolPacket.TYPE, (client, handler, buf, responseSender) -> {
            InitEasyPlaceProtocolPacket packet = new InitEasyPlaceProtocolPacket(buf);
            MinecraftClient.getInstance().execute(() -> {
                PlacementHandlerAccessor.setWhitelistedProperties(packet.whitelistedProperties());
            });
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(SetEasyPlaceProtocolPacket.TYPE)) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                new SetEasyPlaceProtocolPacket(getEasyPlaceProtocol()).write(buf);
                sender.sendPacket(SetEasyPlaceProtocolPacket.TYPE, buf);
                serverHasV3Protocol = true;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlacementHandlerAccessor.setWhitelistedProperties(DEFAULT_WHITELISTED_PROPERTIES);
            serverHasV3Protocol = false;
        });

        Configs.Generic.EASY_PLACE_PROTOCOL.setValueChangeCallback(config -> {
            if (ClientPlayNetworking.canSend(SetEasyPlaceProtocolPacket.TYPE)) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                new SetEasyPlaceProtocolPacket(getEasyPlaceProtocol()).write(buf);
                ClientPlayNetworking.send(SetEasyPlaceProtocolPacket.TYPE, buf);
            }
        });
    }

    private static int getEasyPlaceProtocol() {
        return switch (PlacementHandler.getEffectiveProtocolVersion()) {
            case V3 -> 3;
            case V2 -> 2;
            case SLAB_ONLY -> 1;
            default -> 0;
        };
    }
}
