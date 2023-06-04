package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import net.earthcomputer.litemoretica.client.EasyPlaceProtocolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlacementHandler.class, remap = false)
public class PlacementHandlerMixin_EasyPlaceProtocol {
    @Inject(method = "getEffectiveProtocolVersion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isInSingleplayer()Z", remap = true), cancellable = true)
    private static void allowV3OnLitemoreticaServer(CallbackInfoReturnable<EasyPlaceProtocol> cir) {
        if (EasyPlaceProtocolClient.serverHasV3Protocol) {
            cir.setReturnValue(EasyPlaceProtocol.V3);
        }
    }
}
