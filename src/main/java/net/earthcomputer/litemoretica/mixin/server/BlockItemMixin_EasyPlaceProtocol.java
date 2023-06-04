package net.earthcomputer.litemoretica.mixin.server;

import net.earthcomputer.litemoretica.server.EasyPlaceProtocolServer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockItem.class, priority = 900) // lower priority than litematica's mixin to inject before it
public abstract class BlockItemMixin_EasyPlaceProtocol {
    @Shadow public abstract Block getBlock();

    @Shadow protected abstract boolean canPlace(ItemPlacementContext context, BlockState state);

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void applyEasyPlaceProtocolV3(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (ctx.getPlayer() instanceof ServerPlayerEntity serverPlayer && EasyPlaceProtocolServer.getEasyPlaceProtocol(serverPlayer) == 3) {
            BlockState stateOrig = getBlock().getPlacementState(ctx);
            if (stateOrig != null) {
                BlockState newState = EasyPlaceProtocolServer.applyEasyPlaceProtocolV3(stateOrig, ctx);
                if (canPlace(ctx, newState)) {
                    cir.setReturnValue(newState);
                }
            }
        }
    }
}
