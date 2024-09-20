package net.earthcomputer.litemoretica.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetEasyPlaceProtocolPacket(int protocol) implements CustomPayload {
    public static final Id<SetEasyPlaceProtocolPacket> ID = new Id<>(new Identifier("litemoretica", "set_easy_place_protocol"));
    public static final PacketCodec<RegistryByteBuf, SetEasyPlaceProtocolPacket> CODEC = PacketCodec.of(SetEasyPlaceProtocolPacket::write, SetEasyPlaceProtocolPacket::new);

    private SetEasyPlaceProtocolPacket(PacketByteBuf buf) {
        this(buf.readVarInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeVarInt(protocol);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
