package net.earthcomputer.litemoretica.mixin.client;

import com.google.common.collect.ImmutableSet;
import fi.dy.masa.litematica.util.PlacementHandler;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PlacementHandler.class, remap = false)
public interface PlacementHandlerAccessor {
    @Accessor("WHITELISTED_PROPERTIES")
    @Mutable
    static void setWhitelistedProperties(ImmutableSet<Property<?>> whitelistedProperties) {
        throw new UnsupportedOperationException();
    }
}
