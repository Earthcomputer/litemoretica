package net.earthcomputer.litemoretica.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record SetEasyPlaceProtocolPacket(int protocol) {
    public static final Identifier TYPE = new Identifier("litemoretica", "set_easy_place_protocol");

    public SetEasyPlaceProtocolPacket(PacketByteBuf buf) {
        this(buf.readVarInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(protocol);
    }
}
