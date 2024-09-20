package net.earthcomputer.litemoretica.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PacketSplitter {
    private static final Map<Identifier, PacketCodec<RegistryByteBuf, ?>> CODECS = new HashMap<>();

    @Nullable
    private CustomPayload.Id<?> currentlyReceiving;
    private RegistryByteBuf currentBuf;

    public static <T extends CustomPayload> void register(CustomPayload.Id<T> id, PacketCodec<RegistryByteBuf, T> codec) {
        CODECS.put(id.id(), codec);
    }

    public static <T extends CustomPayload> void registerC2S(CustomPayload.Id<T> id, BiConsumer<T, ServerPlayNetworking.Context> handler) {
        @SuppressWarnings("unchecked")
        var codec = (PacketCodec<RegistryByteBuf, T>) CODECS.get(id.id());
        if (codec == null) {
            throw new IllegalStateException("Registering C2S listener without registering the packet: " + id.id());
        }

        @SuppressWarnings("unchecked")
        var splitPacketId = (CustomPayload.Id<SplitPacket>) id;
        PayloadTypeRegistry.playC2S().register(splitPacketId, SplitPacket.codec(splitPacketId));
        ServerPlayNetworking.registerGlobalReceiver(splitPacketId, (splitPayload, context) -> {
            ((NetHandlerExt) context.player().networkHandler).litemoretica_getPacketSplitter().handle(splitPayload, context.player().getRegistryManager(), codec, packet -> handler.accept(packet, context));
        });
    }

    public static <T extends CustomPayload> void registerS2C(CustomPayload.Id<T> id, BiConsumer<T, ClientPlayNetworking.Context> handler) {
        @SuppressWarnings("unchecked")
        var codec = (PacketCodec<RegistryByteBuf, T>) CODECS.get(id.id());
        if (codec == null) {
            throw new IllegalStateException("Registering S2C listener without registering the packet: " + id.id());
        }

        @SuppressWarnings("unchecked")
        var splitPacketId = (CustomPayload.Id<SplitPacket>) id;
        PayloadTypeRegistry.playS2C().register(splitPacketId, SplitPacket.codec(splitPacketId));
        ClientPlayNetworking.registerGlobalReceiver(splitPacketId, (splitPayload, context) -> {
            ((NetHandlerExt) context.player().networkHandler).litemoretica_getPacketSplitter().handle(splitPayload, context.player().getRegistryManager(), codec, packet -> handler.accept(packet, context));
        });
    }

    public static void sendToServer(CustomPayload packet) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        send(packet, player.getRegistryManager(), Short.MAX_VALUE, ClientPlayNetworking::send);
    }

    public static void sendToClient(ServerPlayerEntity player, CustomPayload packet) {
        send(packet, player.getRegistryManager(), 1048576, splitPayload -> ServerPlayNetworking.send(player, splitPayload));
    }

    private static <T extends CustomPayload> void send(T packet, DynamicRegistryManager registryManager, int batchSize, Consumer<SplitPacket> sender) {
        @SuppressWarnings("unchecked")
        var codec = (PacketCodec<RegistryByteBuf, T>) CODECS.get(packet.getId().id());
        if (codec == null) {
            throw new IllegalStateException("Sending unregistered split packet: " + packet.getId().id());
        }
        @SuppressWarnings("unchecked")
        var splitPacketId = (CustomPayload.Id<SplitPacket>) packet.getId();

        int bytesPerPacket = batchSize - (
            1 // isLast
            + 5 // length of payload
        );

        RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), registryManager);
        try {
            codec.encode(buf, packet);

            while (buf.readableBytes() > bytesPerPacket) {
                byte[] splitPayload = new byte[bytesPerPacket];
                buf.readBytes(splitPayload);
                sender.accept(new SplitPacket(splitPacketId, false, splitPayload));
            }

            byte[] splitPayload = new byte[buf.readableBytes()];
            buf.readBytes(splitPayload);
            sender.accept(new SplitPacket(splitPacketId, true, splitPayload));
        } finally {
            buf.release();
        }
    }

    private <T extends CustomPayload> void handle(SplitPacket splitPacket, DynamicRegistryManager registryManager, PacketCodec<RegistryByteBuf, T> codec, Consumer<T> handler) {
        if (currentlyReceiving != null && currentlyReceiving != splitPacket.id) {
            currentBuf.release();
            currentlyReceiving = null;
            currentBuf = null;
        }

        if (currentlyReceiving == null) {
            currentlyReceiving = splitPacket.id;
            currentBuf = new RegistryByteBuf(Unpooled.buffer(), registryManager);
        }

        currentBuf.writeBytes(splitPacket.payload);

        if (splitPacket.isLast) {
            try {
                T packet = codec.decode(currentBuf);
                handler.accept(packet);
            } finally {
                currentBuf.release();
                currentlyReceiving = null;
                currentBuf = null;
            }
        }
    }

    private record SplitPacket(Id<SplitPacket> id, boolean isLast, byte[] payload) implements CustomPayload {
        public static PacketCodec<RegistryByteBuf, SplitPacket> codec(Id<SplitPacket> id) {
            return PacketCodec.tuple(
                PacketCodecs.BOOL,
                SplitPacket::isLast,
                PacketCodecs.BYTE_ARRAY,
                SplitPacket::payload,
                (isLast, payload) -> new SplitPacket(id, isLast, payload)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return id;
        }
    }

    public interface NetHandlerExt {
        PacketSplitter litemoretica_getPacketSplitter();
    }
}
