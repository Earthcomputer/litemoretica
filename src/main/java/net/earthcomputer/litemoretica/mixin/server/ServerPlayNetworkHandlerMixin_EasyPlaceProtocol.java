package net.earthcomputer.litemoretica.mixin.server;

import net.earthcomputer.litemoretica.server.EasyPlaceProtocolServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 900) // lower priority than carpet's and litematica's mixins so that the redirect is only applied if neither have it
public class ServerPlayNetworkHandlerMixin_EasyPlaceProtocol implements EasyPlaceProtocolServer.NetworkHandlerExt {
    @Unique
    private int litemoretica_easyPlaceProtocol;

    @Override
    public int litemoretica_getEasyPlaceProtocol() {
        return litemoretica_easyPlaceProtocol;
    }

    @Override
    public void litemoretica_setEasyPlaceProtocol(int easyPlaceProtocol) {
        litemoretica_easyPlaceProtocol = easyPlaceProtocol;
    }

    @Redirect(method = "onPlayerInteractBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;subtract(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"), require = 0)
    private Vec3d removeHitPosCheck(Vec3d hitVec, Vec3d blockCenter) {
        return Vec3d.ZERO;
    }
}
