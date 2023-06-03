package net.earthcomputer.litemoretica.client;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.earthcomputer.litemoretica.mixin.client.SchematicUtilsAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class LitemoreticaSchematicUtils {
    private LitemoreticaSchematicUtils() {
    }

    public static boolean saveAreaSelectionToSchematic(World mcWorld) {
        SelectionManager selectionManager = DataManager.getSelectionManager();
        AreaSelection currentSelection = selectionManager.getCurrentSelection();
        if (currentSelection == null) {
            return false;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }

        for (Box subregion : currentSelection.getAllSubRegionBoxes()) {
            BlockPos pos1 = subregion.getPos1();
            BlockPos pos2 = subregion.getPos2();
            if (pos1 == null || pos2 == null) {
                continue;
            }

            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int y = Math.min(pos1.getY(), pos2.getY()), yEnd = Math.max(pos1.getY(), pos2.getY()); y <= yEnd; y++) {
                for (int x = Math.min(pos1.getX(), pos2.getX()), xEnd = Math.max(pos1.getX(), pos2.getX()); x <= xEnd; x++) {
                    for (int z = Math.min(pos1.getZ(), pos2.getZ()), zEnd = Math.max(pos1.getZ(), pos2.getZ()); z <= zEnd; z++) {
                        pos.set(x, y, z);
                        BlockState worldState = mcWorld.getBlockState(pos);
                        BlockState schematicState = schematicWorld.getBlockState(pos);
                        if (worldState != schematicState) {
                            SchematicUtilsAccessor.invokeSetTargetedSchematicBlockState(pos, worldState);
                        }
                    }
                }
            }
        }

        return true;
    }
}
