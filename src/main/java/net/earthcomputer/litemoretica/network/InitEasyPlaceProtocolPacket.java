package net.earthcomputer.litemoretica.network;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashSet;
import java.util.Set;

public record InitEasyPlaceProtocolPacket(ImmutableSet<Property<?>> whitelistedProperties) {
    public static final Identifier TYPE = new Identifier("litemoretica", "init_easy_place");

    public InitEasyPlaceProtocolPacket(PacketByteBuf buf) {
        this(readWhitelistedProperties(buf));
    }

    public void write(PacketByteBuf buf) {
        writeWhitelistedProperties(buf, whitelistedProperties);
    }

    private static ImmutableSet<Property<?>> readWhitelistedProperties(PacketByteBuf buf) {
        int numProperties = buf.readVarInt();
        ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builderWithExpectedSize(numProperties);
        for (int i = 0; i < numProperties; i++) {
            Identifier blockId = buf.readIdentifier();
            Block block = Registry.BLOCK.get(blockId);
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
        for (Block block : Registry.BLOCK) {
            for (Property<?> property : block.getStateManager().getProperties()) {
                if (propertiesToWrite.remove(property)) {
                    buf.writeIdentifier(Registry.BLOCK.getId(block));
                    buf.writeString(property.getName(), 256);
                    if (propertiesToWrite.isEmpty()) {
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("Found properties with no block containing them: " + propertiesToWrite);
    }
}
