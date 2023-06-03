package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.config.Configs;
import net.earthcomputer.litemoretica.client.EasyPlaceFix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin_EasyPlaceFix {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && !EasyPlaceFix.isPlacingWithEasyPlace) {
            if (EasyPlaceFix.handleEasyPlaceRestriction(client)) {
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }
}
