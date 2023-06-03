package net.earthcomputer.litemoretica.client;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class LitemoreticaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(() -> {
            LitemoreticaHotkeys.addCallbacks(MinecraftClient.getInstance());
        });
    }
}
