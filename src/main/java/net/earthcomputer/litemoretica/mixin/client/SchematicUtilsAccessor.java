package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.util.SchematicUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = SchematicUtils.class, remap = false)
public interface SchematicUtilsAccessor {
    @Invoker
    static boolean invokeSetTargetedSchematicBlockState(BlockPos pos, BlockState state) {
        throw new UnsupportedOperationException();
    }
}
