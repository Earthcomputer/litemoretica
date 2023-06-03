package net.earthcomputer.litemoretica.client;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.MinecraftClient;

public final class LitemoreticaHotkeys {
    public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_SELECTION = new ConfigHotkey("schematicEditReplaceSelection", "", "A hotkey to copy the blocks in the world within the area selection into the litematic");

    private LitemoreticaHotkeys() {
    }

    public static ConfigHotkey[] getExtraHotkeys() {
        return new ConfigHotkey[] {
            SCHEMATIC_EDIT_REPLACE_SELECTION,
        };
    }

    public static void addCallbacks(MinecraftClient mc) {
        Callback callback = new Callback(mc);

        SCHEMATIC_EDIT_REPLACE_SELECTION.getKeybind().setCallback(callback);
    }

    private static final class Callback implements IHotkeyCallback {
        private final MinecraftClient mc;

        private Callback(MinecraftClient mc) {
            this.mc = mc;
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            if (mc.player == null || mc.world == null) {
                return false;
            }

            if (key == SCHEMATIC_EDIT_REPLACE_SELECTION.getKeybind()) {
                return LitemoreticaSchematicUtils.saveAreaSelectionToSchematic(mc.world);
            }

            return false;
        }
    }
}
