package net.earthcomputer.litemoretica.network;

import com.google.common.collect.ImmutableSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public record InitEasyPlaceProtocolPacket(ImmutableSet<Property<?>> whitelistedProperties) {
    public static final Identifier TYPE = new Identifier("litemoretica", "init_easy_place");

    private static final Registry<Block> BLOCK_REGISTRY = getBlockRegistry();

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
            Block block = BLOCK_REGISTRY.get(blockId);
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
        for (Block block : BLOCK_REGISTRY) {
            for (Property<?> property : block.getStateManager().getProperties()) {
                if (propertiesToWrite.remove(property)) {
                    buf.writeIdentifier(BLOCK_REGISTRY.getId(block));
                    buf.writeString(property.getName(), 256);
                    if (propertiesToWrite.isEmpty()) {
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("Found properties with no block containing them: " + propertiesToWrite);
    }

    // TODO: remove in 1.20
    @SuppressWarnings("unchecked")
    private static Registry<Block> getBlockRegistry() {
        try {
            return LazyGetBlockRegistry.getBlockRegistry();
        } catch (NoClassDefFoundError e) {
            try {
                String fieldName = FabricLoader.getInstance().getMappingResolver().mapFieldName(
                    "intermediary",
                    "net.minecraft.class_2378",
                    "field_11146",
                    "Lnet/minecraft/class_2348;"
                );
                return (Registry<Block>) Registry.class.getField(fieldName).get(null);
            } catch (ReflectiveOperationException e1) {
                throw e;
            }
        }
    }

    private static class LazyGetBlockRegistry {
        private static Registry<Block> getBlockRegistry() {
            return Registries.BLOCK;
        }
    }
}
