package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldUtils.class)
public interface WorldUtilsAccessor {
    @Invoker
    static boolean invokePlacementRestrictionInEffect(MinecraftClient mc) {
        throw new UnsupportedOperationException();
    }
}
