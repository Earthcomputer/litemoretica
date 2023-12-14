package net.earthcomputer.litemoretica.client;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;

public final class LitemoreticaConfigs {
    private LitemoreticaConfigs() {
    }

    public static final ConfigBoolean MATERIAL_LIST_IGNORE_BLOCK_STATE = new ConfigBoolean("materialListIgnoreBlockState", true, "If true, ignores wrong blockstates\nwhen generating material lists");

    public static IConfigBase[] getExtraGenericConfigs() {
        return new IConfigBase[] {
            MATERIAL_LIST_IGNORE_BLOCK_STATE,
        };
    }
}
