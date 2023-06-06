package net.earthcomputer.litemoretica.network;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registry;

// TODO: remove in 1.20
public class Registries {
    public static final Registry<Block> BLOCK;
    public static final Registry<Fluid> FLUID;

    static {
        Registry<Block> block;
        Registry<Fluid> fluid;
        try {
            block = Lazy1_19_4.getBlockRegistry();
            fluid = Lazy1_19_4.getFluidRegistry();
        } catch (NoClassDefFoundError e) {
            block = getDefaultedRegistry1_19_2("field_11146");
            fluid = getDefaultedRegistry1_19_2("field_11154");
        }
        BLOCK = block;
        FLUID = fluid;
    }

    @SuppressWarnings("unchecked")
    private static <T> Registry<T> getDefaultedRegistry1_19_2(String intermediary) {
        try {
            String fieldName = FabricLoader.getInstance().getMappingResolver().mapFieldName(
                "intermediary",
                "net.minecraft.class_2378",
                intermediary,
                "Lnet/minecraft/class_2348;"
            );
            return (Registry<T>) Registry.class.getField(fieldName).get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Lazy1_19_4 {
        static Registry<Block> getBlockRegistry() {
            return net.minecraft.registry.Registries.BLOCK;
        }

        static Registry<Fluid> getFluidRegistry() {
            return net.minecraft.registry.Registries.FLUID;
        }
    }
}
