package net.earthcomputer.litemoretica.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class LitemoreticaMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;

    @Override
    public void onLoad(String mixinPackage) {
        enabled = isEnabled(mixinPackage.contains("client"));
    }

    private static boolean isEnabled(boolean client) {
        if (client) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
                return false;
            }
            if (!FabricLoader.getInstance().isModLoaded("litematica")) {
                throw new IllegalStateException("litemoretica requires litematica on the client, but it is not installed. Please install litematica");
            }
            return true;
        }

        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
