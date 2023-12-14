package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.config.Configs;
import net.earthcomputer.litemoretica.client.LitemoreticaConfigs;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = Configs.Generic.class, remap = false)
public class ConfigsGenericMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"))
    private static Object[] modifyConfigs(Object[] configs) {
        return ArrayUtils.addAll(configs, (Object[]) LitemoreticaConfigs.getExtraGenericConfigs());
    }
}
