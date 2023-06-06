package net.earthcomputer.litemoretica.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PacketSplitter {
    @Nullable
    private Identifier currentlyReceiving;
    private int partsLeftToReceive;
    private PacketByteBuf currentBuf;

    public static <T extends SplitPacket> void registerC2S(SplitPacketType<T> type, BiConsumer<T, ServerPlayNetworkHandler> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type.id, (server, player, handler1, buf, responseSender) -> {
            ((NetHandlerExt) handler1).litemoretica_getPacketSplitter().handle(buf, type, packet -> handler.accept(packet, handler1), handler1::disconnect);
        });
    }

    public static <T extends SplitPacket> void registerS2C(SplitPacketType<T> type, Consumer<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type.id, (client, handler1, buf, responseSender) -> {
            ((NetHandlerExt) handler1).litemoretica_getPacketSplitter().handle(buf, type, handler, handler1.getConnection()::disconnect);
        });
    }

    public static void sendToServer(SplitPacket packet) {
        send(packet, Short.MAX_VALUE, ClientPlayNetworking::send);
    }

    public static void sendToClient(ServerPlayerEntity player, SplitPacket packet) {
        send(packet, 1048576, (id, buf) -> ServerPlayNetworking.send(player, id, buf));
    }

    private static void send(SplitPacket packet, int batchSize, BiConsumer<Identifier, PacketByteBuf> sender) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            packet.write(buf);
            int numPackets = MathHelper.ceilDiv(buf.writerIndex() + 5, batchSize);

            PacketByteBuf buf2 = new PacketByteBuf(Unpooled.buffer());
            buf2.writeVarInt(numPackets);
            buf2.writeBytes(buf, Math.min(buf.readableBytes(), batchSize - 5));
            sender.accept(packet.getType().id, buf2);
            while (buf.isReadable()) {
                buf2 = new PacketByteBuf(Unpooled.buffer());
                buf2.writeBytes(buf, Math.min(buf.readableBytes(), batchSize));
                sender.accept(packet.getType().id, buf2);
            }
        } finally {
            buf.release();
        }
    }

    public <T extends SplitPacket> void handle(PacketByteBuf buf, SplitPacketType<T> type, Consumer<T> handler, Consumer<Text> disconnecter) {
        if (currentlyReceiving == null) {
            partsLeftToReceive = buf.readVarInt() - 1;
            if (partsLeftToReceive == 0) {
                handler.accept(type.deserializer.apply(buf));
            } else {
                currentBuf = new PacketByteBuf(Unpooled.buffer());
                currentBuf.writeBytes(buf);
                currentlyReceiving = type.id;
            }
        } else {
            if (!currentlyReceiving.equals(type.id)) {
                disconnecter.accept(Text.literal("Invalid split packet"));
            }
            currentBuf.writeBytes(buf);
            if (--partsLeftToReceive == 0) {
                try {
                    handler.accept(type.deserializer.apply(currentBuf));
                } finally {
                    currentlyReceiving = null;
                    currentBuf.release();
                    currentBuf = null;
                }
            }
        }
    }

    // TODO: remove in 1.20 (replaced with FabricPacket)
    public interface SplitPacket {
        void write(PacketByteBuf buf);
        SplitPacketType<?> getType();
    }

    public record SplitPacketType<T extends SplitPacket>(Identifier id, Function<PacketByteBuf, T> deserializer) {
        public static <T extends SplitPacket> SplitPacketType<T> create(Identifier id, Function<PacketByteBuf, T> deserializer) {
            return new SplitPacketType<>(id, deserializer);
        }
    }

    public interface NetHandlerExt {
        PacketSplitter litemoretica_getPacketSplitter();
    }
}
