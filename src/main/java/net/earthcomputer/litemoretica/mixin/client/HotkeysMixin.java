package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.config.Hotkeys;
import net.earthcomputer.litemoretica.client.LitemoreticaHotkeys;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = Hotkeys.class, remap = false)
public class HotkeysMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"))
    private static Object[] modifyHotkeys(Object[] hotkeys) {
        return ArrayUtils.addAll(hotkeys, (Object[]) LitemoreticaHotkeys.getExtraHotkeys());
    }
}
