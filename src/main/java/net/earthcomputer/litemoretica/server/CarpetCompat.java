package net.earthcomputer.litemoretica.server;

import carpet.CarpetSettings;
import net.fabricmc.loader.api.FabricLoader;

public final class CarpetCompat {
    private static final boolean HAS_CARPET = FabricLoader.getInstance().isModLoaded("carpet");

    private CarpetCompat() {
    }

    public static void onFillUpdatesSkipStart() {
        if (HAS_CARPET) {
            CarpetSettings.impendingFillSkipUpdates.set(!CarpetSettings.fillUpdates);
        }
    }

    public static void onFillUpdatesSkipEnd() {
        if (HAS_CARPET) {
            CarpetSettings.impendingFillSkipUpdates.set(false);
        }
    }
}
