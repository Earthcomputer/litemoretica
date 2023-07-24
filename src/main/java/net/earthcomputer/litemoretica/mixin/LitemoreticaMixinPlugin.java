package net.earthcomputer.litemoretica.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class LitemoreticaMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;
    @Nullable
    private Version litematicaVersion;

    private static final Version V0_15_3;
    static {
        try {
            V0_15_3 = Version.parse("0.15.3");
        } catch (VersionParsingException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        litematicaVersion = FabricLoader.getInstance().getModContainer("litematica").map(mod -> mod.getMetadata().getVersion()).orElse(null);
        enabled = isEnabled(mixinPackage.contains("client"));
    }

    private boolean isEnabled(boolean client) {
        if (client) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
                return false;
            }
            if (litematicaVersion == null) {
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
        if (!enabled) {
            return false;
        }

        if ("net.earthcomputer.litemoretica.mixin.client.RayTraceUtilsMixin_ShapeContextFix".equals(mixinClassName)
            && litematicaVersion != null
            && litematicaVersion.compareTo(V0_15_3) >= 0) {
            return false;
        }

        return true;
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
