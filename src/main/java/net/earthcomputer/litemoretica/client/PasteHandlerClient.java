package net.earthcomputer.litemoretica.client;

import com.google.common.collect.ArrayListMultimap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkBase;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.NBTUtils;
import net.earthcomputer.litemoretica.network.PacketSplitter;
import net.earthcomputer.litemoretica.network.UploadChunkPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.world.tick.OrderedTick;

import java.util.*;

public class PasteHandlerClient extends TaskPasteSchematicPerChunkBase {
    private final ArrayListMultimap<ChunkPos, SchematicPlacement> placementsPerChunk = ArrayListMultimap.create();

    public PasteHandlerClient(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly) {
        super(placements, range, changedBlocksOnly);
    }

    public static boolean canRun() {
        return LitemoreticaClient.HAS_NETWORKING && ClientPlayNetworking.canSend(UploadChunkPacket.TYPE.id());
    }

    @Override
    public boolean canExecute() {
        return super.canExecute() && canRun();
    }

    @Override
    protected void onChunkAddedForHandling(ChunkPos pos, SchematicPlacement placement) {
        super.onChunkAddedForHandling(pos, placement);
        placementsPerChunk.put(pos, placement);
    }

    @Override
    public boolean execute() {
        if (ignoreBlocks && ignoreEntities) {
            return true;
        }

        sortChunkList();

        for (int chunkIndex = 0; chunkIndex < pendingChunks.size(); chunkIndex++) {
            ChunkPos pos = pendingChunks.get(chunkIndex);
            if (canProcessChunk(pos) && processChunk(pos)) {
                pendingChunks.remove(chunkIndex);
                break;
            }
        }

        if (pendingChunks.isEmpty()) {
            finished = true;
            return true;
        }

        updateInfoHudLines();

        return false;
    }

    @Override
    protected boolean processChunk(ChunkPos pos) {
        for (SchematicPlacement placementMain : placementsPerChunk.removeAll(pos)) {
            placementMain.getBoxesWithinChunk(pos.x, pos.z).forEach((regionName, box) -> {
                processSubPlacement(placementMain, regionName, box);
            });
        }
        return true;
    }

    @Override
    protected void onStop() {
        if (finished) {
            InfoUtils.showGuiOrActionBarMessage(Message.MessageType.SUCCESS, "litematica.message.schematic_pasted");
        } else {
            InfoUtils.showGuiOrActionBarMessage(Message.MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.onStop();
    }

    private static void processSubPlacement(SchematicPlacement placementMain, String regionName, IntBoundingBox box) {
        SubRegionPlacement placementSub = placementMain.getRelativeSubRegionPlacement(regionName);
        Vec3i regionSize = placementMain.getSchematic().getAreaSize(regionName);
        LitematicaBlockStateContainer container = placementMain.getSchematic().getSubRegionContainer(regionName);
        Map<BlockPos, NbtCompound> blockEntityMap = placementMain.getSchematic().getBlockEntityMapForRegion(regionName);
        List<LitematicaSchematic.EntityInfo> entityList = placementMain.getSchematic().getEntityListForRegion(regionName);
        Map<BlockPos, OrderedTick<Block>> scheduledBlockTicks = placementMain.getSchematic().getScheduledBlockTicksForRegion(regionName);
        Map<BlockPos, OrderedTick<Fluid>> scheduledFluidTicks = placementMain.getSchematic().getScheduledFluidTicksForRegion(regionName);

        if (placementSub == null || regionSize == null || container == null) {
            return;
        }

        BlockRotation rotationCombined = placementMain.getRotation().rotate(placementSub.getRotation());
        BlockMirror mirrorMain = placementMain.getMirror();
        BlockMirror mirrorSub = placementSub.getMirror();
        boolean ignoreInventories = Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue();

        if (mirrorSub != BlockMirror.NONE && (placementMain.getRotation() == BlockRotation.CLOCKWISE_90 || placementMain.getRotation() == BlockRotation.COUNTERCLOCKWISE_90)) {
            mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
        }

        BlockPos regionPos = placementSub.getPos();

        // These are the untransformed relative positions
        BlockPos posEndRel = (new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize))).add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, mirrorMain, placementMain.getRotation());

        int posMinRelMinusRegX = posMinRel.getX() - regionPos.getX();
        int posMinRelMinusRegY = posMinRel.getY() - regionPos.getY();
        int posMinRelMinusRegZ = posMinRel.getZ() - regionPos.getZ();

        UploadChunkPacket.ReplaceBehavior replaceBehavior = switch ((ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue()) {
            case NONE -> UploadChunkPacket.ReplaceBehavior.NONE;
            case ALL -> UploadChunkPacket.ReplaceBehavior.ALL;
            case WITH_NON_AIR -> UploadChunkPacket.ReplaceBehavior.WITH_NON_AIR;
        };

        BlockPos minPos = new BlockPos(box.minX, box.minY, box.minZ);
        BlockPos maxPos = new BlockPos(box.maxX, box.maxY, box.maxZ);

        BlockState[] blockData = new BlockState[(box.maxX - box.minX + 1) * (box.maxY - box.minY + 1) * (box.maxZ - box.minZ + 1)];
        List<NbtCompound> blockEntities = new ArrayList<>();
        List<OrderedTick<Block>> blockTicks = new ArrayList<>();
        List<OrderedTick<Fluid>> fluidTicks = new ArrayList<>();

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        int blockIndex = 0;
        for (int y = box.minY; y <= box.maxY; y++) {
            for (int z = box.minZ; z <= box.maxZ; z++) {
                for (int x = box.minX; x <= box.maxX; x++) {
                    mutablePos.set(
                            x - placementMain.getOrigin().getX() - regionPosTransformed.getX(),
                            y - placementMain.getOrigin().getY() - regionPosTransformed.getY(),
                            z - placementMain.getOrigin().getZ() - regionPosTransformed.getZ());
                    mutablePos.set(PositionUtils.getReverseTransformedBlockPos(mutablePos, placementSub.getMirror(), placementSub.getRotation()));
                    mutablePos.set(PositionUtils.getReverseTransformedBlockPos(mutablePos, placementMain.getMirror(), placementMain.getRotation()));
                    mutablePos.set(mutablePos.getX() - posMinRelMinusRegX, mutablePos.getY() - posMinRelMinusRegY, mutablePos.getZ() - posMinRelMinusRegZ);

                    BlockState state = container.get(mutablePos.getX(), mutablePos.getY(), mutablePos.getZ());
                    if (mirrorMain != BlockMirror.NONE) {
                        state = state.mirror(mirrorMain);
                    }
                    if (mirrorSub != BlockMirror.NONE) {
                        state = state.mirror(mirrorSub);
                    }
                    if (rotationCombined != BlockRotation.NONE) {
                        state = state.rotate(rotationCombined);
                    }
                    blockData[blockIndex++] = state;

                    if (blockEntityMap != null) {
                        NbtCompound beNbt = blockEntityMap.get(mutablePos);
                        if (beNbt != null) {
                            beNbt = beNbt.copy();
                            beNbt.putInt("x", x);
                            beNbt.putInt("y", y);
                            beNbt.putInt("z", z);
                            if (ignoreInventories) {
                                beNbt.remove("Items");
                            }
                            blockEntities.add(beNbt);
                        }
                    }

                    if (scheduledBlockTicks != null) {
                        OrderedTick<Block> blockTick = scheduledBlockTicks.get(mutablePos);
                        if (blockTick != null) {
                            blockTicks.add(new OrderedTick<>(blockTick.type(), new BlockPos(x, y, z), blockTick.triggerTick(), blockTick.priority(), blockTick.subTickOrder()));
                        }
                    }
                    if (scheduledFluidTicks != null) {
                        OrderedTick<Fluid> fluidTick = scheduledFluidTicks.get(mutablePos);
                        if (fluidTick != null) {
                            fluidTicks.add(new OrderedTick<>(fluidTick.type(), new BlockPos(x, y, z), fluidTick.triggerTick(), fluidTick.priority(), fluidTick.subTickOrder()));
                        }
                    }
                }
            }
        }

        List<NbtCompound> entities = new ArrayList<>();
        if (entityList != null) {
            for (LitematicaSchematic.EntityInfo info : entityList) {
                Vec3d entityPos = info.posVec;
                entityPos = PositionUtils.getTransformedPosition(entityPos, placementMain.getMirror(), placementMain.getRotation());
                entityPos = PositionUtils.getTransformedPosition(entityPos, placementSub.getMirror(), placementSub.getRotation());
                entityPos = entityPos.add(
                        regionPosTransformed.getX() + placementMain.getOrigin().getX(),
                        regionPosTransformed.getY() + placementMain.getOrigin().getY(),
                        regionPosTransformed.getZ() + placementMain.getOrigin().getZ());
                if (box.containsPos(BlockPos.ofFloored(entityPos))) {
                    NbtCompound entityTag = info.nbt.copy();
                    NBTUtils.writeEntityPositionToTag(entityPos, entityTag);
                    if (entityTag.contains("TileX", NbtElement.NUMBER_TYPE)) {
                        entityTag.putInt("TileX", MathHelper.floor(entityPos.getX()));
                    }
                    if (entityTag.contains("TileY", NbtElement.NUMBER_TYPE)) {
                        entityTag.putInt("TileY", MathHelper.floor(entityPos.getY()));
                    }
                    if (entityTag.contains("TileZ", NbtElement.NUMBER_TYPE)) {
                        entityTag.putInt("TileZ", MathHelper.floor(entityPos.getZ()));
                    }
                    entities.add(entityTag);
                }
            }
        }

        UploadChunkPacket packet = new UploadChunkPacket(replaceBehavior, minPos, maxPos, blockData, blockEntities, entities, mirrorMain, mirrorSub, rotationCombined, blockTicks, fluidTicks);
        PacketSplitter.sendToServer(packet);
    }
}
