package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.util.LayerRange;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(WorldUtils.class)
public class WorldUtilsMixin_EasyPlaceFix {
    @Inject(method = "placementRestrictionInEffect", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/materials/MaterialCache;getInstance()Lfi/dy/masa/litematica/materials/MaterialCache;"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private static void stopEasyPlaceWhenBlockAlreadyCorrect(MinecraftClient mc, CallbackInfoReturnable<Boolean> cir, HitResult trace, ItemStack stack, BlockHitResult blockHitResult, ItemPlacementContext ctx, BlockPos pos, BlockState stateClient, World worldSchematic, LayerRange range, boolean schematicHasAir, BlockState stateSchematic) {
        if (stateClient == stateSchematic) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
