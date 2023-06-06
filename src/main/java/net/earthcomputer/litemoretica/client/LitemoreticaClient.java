package net.earthcomputer.litemoretica.client;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public class LitemoreticaClient implements ClientModInitializer {
    public static final boolean HAS_NETWORKING = FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1");

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(() -> {
            LitemoreticaHotkeys.addCallbacks(MinecraftClient.getInstance());
        });

        if (HAS_NETWORKING) {
            EasyPlaceProtocolClient.init();
        }
    }
}
