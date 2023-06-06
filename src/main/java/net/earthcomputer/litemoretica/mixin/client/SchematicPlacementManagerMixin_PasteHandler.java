package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import net.earthcomputer.litemoretica.client.PasteHandlerClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(value = SchematicPlacementManager.class, remap = false)
public class SchematicPlacementManagerMixin_PasteHandler {
    @Inject(method = "pastePlacementToWorld(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacement;ZZLnet/minecraft/client/MinecraftClient;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isIntegratedServerRunning()Z", remap = true), remap = true, cancellable = true)
    private void useOurPasteHandling(SchematicPlacement schematicPlacement, boolean changedBlocksOnly, boolean printMessage, MinecraftClient mc, CallbackInfo ci) {
        if (PasteHandlerClient.canRun()) {
            PasteHandlerClient task = new PasteHandlerClient(Collections.singletonList(schematicPlacement), DataManager.getRenderLayerRange(), changedBlocksOnly);
            TaskScheduler.getInstanceClient().scheduleTask(task, Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue());
            if (printMessage) {
                InfoUtils.showGuiOrActionBarMessage(Message.MessageType.INFO, "litematica.message.scheduled_task_added");
            }
            ci.cancel();
        }
    }
}
