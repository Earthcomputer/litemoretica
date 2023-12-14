package net.earthcomputer.litemoretica.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement;
import net.earthcomputer.litemoretica.client.LitemoreticaConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TaskCountBlocksPlacement.class, remap = false)
public abstract class TaskCountBlocksPlacementMixin_MaterialListFeature extends TaskCountBlocksBase {
  @Unique private boolean ignoreState;

  protected TaskCountBlocksPlacementMixin_MaterialListFeature(IMaterialList materialList, String nameOnHud) {
    super(materialList, nameOnHud);
  }

  @Inject(method = "<init>", at = @At(value = "RETURN"))
  private void onConstructor(CallbackInfo info) {
    this.ignoreState = LitemoreticaConfigs.MATERIAL_LIST_IGNORE_BLOCK_STATE.getBooleanValue();
  }

  @Inject(method = "countAtPosition", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/scheduler/tasks/TaskCountBlocksPlacement;countsMissing:Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;"), cancellable = true)
  protected void countAtPosition(BlockPos pos, CallbackInfo ci, @Local(name = "stateSchematic") BlockState stateSchematic, @Local(name = "stateClient") BlockState stateClient) {
      if (!stateClient.isAir() && ignoreState && stateClient.getBlock() == stateSchematic.getBlock()) {
          ci.cancel();
      }
  }
}
