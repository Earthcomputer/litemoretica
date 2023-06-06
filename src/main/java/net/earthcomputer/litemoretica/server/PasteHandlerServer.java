package net.earthcomputer.litemoretica.server;

import net.earthcomputer.litemoretica.network.PacketSplitter;
import net.earthcomputer.litemoretica.network.UploadChunkPacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.tick.OrderedTick;

public final class PasteHandlerServer {
    private PasteHandlerServer() {
    }

    public static void init() {
        PacketSplitter.registerC2S(UploadChunkPacket.TYPE, (packet, handler) -> {
            handler.player.server.execute(() -> {
                CarpetCompat.onFillUpdatesSkipStart();
                try {
                    handleChunkUpload(packet, handler);
                } finally {
                    CarpetCompat.onFillUpdatesSkipEnd();
                }
            });
        });
    }

    private static void handleChunkUpload(UploadChunkPacket packet, ServerPlayNetworkHandler handler) {
        if (!handler.player.isCreative()) {
            return;
        }

        int chunkX = packet.minPos.getX() >> 4;
        int chunkZ = packet.minPos.getZ() >> 4;
        if ((packet.maxPos.getX() >> 4) != chunkX || (packet.maxPos.getZ() >> 4) != chunkZ) {
            return;
        }

        ServerWorld world = handler.player.getWorld();

        BlockBox box = BlockBox.create(packet.minPos, packet.maxPos);

        // load blocks
        int blockIndex = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
            for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                    BlockState blockToPlace = packet.blockData[blockIndex++];
                    if (blockToPlace.isOf(Blocks.STRUCTURE_VOID)) {
                        continue;
                    }
                    BlockState oldState = world.getBlockState(pos.set(x, y, z));
                    if ((packet.replaceBehavior == UploadChunkPacket.ReplaceBehavior.NONE && !oldState.isAir()) || (packet.replaceBehavior == UploadChunkPacket.ReplaceBehavior.WITH_NON_AIR && blockToPlace.isAir())) {
                        continue;
                    }
                    world.setBlockState(pos, blockToPlace, Block.FORCE_STATE | Block.NOTIFY_LISTENERS);
                }
            }
        }

        // load block entities
        for (NbtCompound blockEntity : packet.blockEntities) {
            int x = blockEntity.getInt("x");
            int y = blockEntity.getInt("y");
            int z = blockEntity.getInt("z");
            blockEntity.remove("id");

            if (!box.contains(x, y, z)) {
                continue;
            }

            BlockEntity be = world.getBlockEntity(pos.set(x, y, z));
            if (be != null) {
                be.readNbt(blockEntity);
            }
        }

        // load entities (and remove old ones)
        world.getOtherEntities(null, Box.from(box), entity -> {
            return box.contains(entity.getBlockPos());
        }).forEach(entity -> entity.remove(Entity.RemovalReason.DISCARDED));
        for (NbtCompound entity : packet.entities) {
            entity.remove(Entity.UUID_KEY);
            Entity loadedEntity = EntityType.loadEntityWithPassengers(entity, world, ent -> {
                float yaw = ent.getYaw();
                if (packet.entityMirror1 != BlockMirror.NONE) {
                    yaw = ent.applyMirror(packet.entityMirror1);
                }
                if (packet.entityMirror2 != BlockMirror.NONE) {
                    yaw = ent.applyMirror(packet.entityMirror2);
                }
                if (packet.entityRotation != BlockRotation.NONE) {
                    yaw = ent.applyRotation(packet.entityRotation);
                }
                ent.refreshPositionAndAngles(ent.getBlockPos(), yaw, ent.getPitch());
                if (ent instanceof LivingEntity living) {
                    living.headYaw = yaw;
                    living.bodyYaw = yaw;
                    living.prevHeadYaw = yaw;
                    living.prevBodyYaw = yaw;
                }
                return ent;
            });
            if (loadedEntity != null) {
                if (box.contains(loadedEntity.getBlockPos())) {
                    world.spawnNewEntityAndPassengers(loadedEntity);
                }
            }
        }

        // load scheduled ticks
        world.getBlockTickScheduler().clearNextTicks(box);
        for (OrderedTick<Block> blockTick : packet.scheduledBlockTicks) {
            if (box.contains(blockTick.pos())) {
                world.getBlockTickScheduler().scheduleTick(blockTick);
            }
        }
        world.getFluidTickScheduler().clearNextTicks(box);
        for (OrderedTick<Fluid> fluidTick : packet.scheduledFluidTicks) {
            if (box.contains(fluidTick.pos())) {
                world.getFluidTickScheduler().scheduleTick(fluidTick);
            }
        }
    }
}
