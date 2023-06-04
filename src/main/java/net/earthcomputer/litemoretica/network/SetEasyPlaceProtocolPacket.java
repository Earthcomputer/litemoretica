package net.earthcomputer.litemoretica.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record SetEasyPlaceProtocolPacket(int protocol) implements FabricPacket {
    public static final PacketType<SetEasyPlaceProtocolPacket> TYPE = PacketType.create(new Identifier("litemoretica", "set_easy_place_protocol"), SetEasyPlaceProtocolPacket::new);

    public SetEasyPlaceProtocolPacket(PacketByteBuf buf) {
        this(buf.readVarInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(protocol);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
