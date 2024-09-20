package net.earthcomputer.litemoretica.network;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public record InitEasyPlaceProtocolPacket(ImmutableSet<Property<?>> whitelistedProperties) implements CustomPayload {
    public static final Id<InitEasyPlaceProtocolPacket> ID = new Id<>(new Identifier("litemoretica", "init_easy_place"));
    public static final PacketCodec<RegistryByteBuf, InitEasyPlaceProtocolPacket> CODEC = PacketCodec.of(InitEasyPlaceProtocolPacket::write, InitEasyPlaceProtocolPacket::new);

    private InitEasyPlaceProtocolPacket(PacketByteBuf buf) {
        this(readWhitelistedProperties(buf));
    }

    private void write(PacketByteBuf buf) {
        writeWhitelistedProperties(buf, whitelistedProperties);
    }

    private static ImmutableSet<Property<?>> readWhitelistedProperties(PacketByteBuf buf) {
        int numProperties = buf.readVarInt();
        ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builderWithExpectedSize(numProperties);
        for (int i = 0; i < numProperties; i++) {
            Identifier blockId = buf.readIdentifier();
            Block block = Registries.BLOCK.get(blockId);
            String propertyName = buf.readString(256);
            Property<?> property = block.getStateManager().getProperty(propertyName);
            if (property != null) {
                properties.add(property);
            }
        }
        return properties.build();
    }

    private static void writeWhitelistedProperties(PacketByteBuf buf, ImmutableSet<Property<?>> whitelistedProperties) {
        buf.writeVarInt(whitelistedProperties.size());
        Set<Property<?>> propertiesToWrite = new HashSet<>(whitelistedProperties);
        for (Block block : Registries.BLOCK) {
            for (Property<?> property : block.getStateManager().getProperties()) {
                if (propertiesToWrite.remove(property)) {
                    buf.writeIdentifier(Registries.BLOCK.getId(block));
                    buf.writeString(property.getName(), 256);
                    if (propertiesToWrite.isEmpty()) {
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("Found properties with no block containing them: " + propertiesToWrite);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
