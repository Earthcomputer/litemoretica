package net.earthcomputer.litemoretica.network;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.TickPriority;
import net.minecraft.world.tick.OrderedTick;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UploadChunkPacket implements PacketSplitter.SplitPacket {
    public static final PacketSplitter.SplitPacketType<UploadChunkPacket> TYPE = PacketSplitter.SplitPacketType.create(new Identifier("litemoretica", "upload_chunk"), UploadChunkPacket::new);

    public final ReplaceBehavior replaceBehavior;
    public final BlockPos minPos;
    public final BlockPos maxPos;
    public final BlockState[] blockData;
    public final NbtCompound[] blockEntities;
    public final NbtCompound[] entities;
    public final BlockMirror entityMirror1;
    public final BlockMirror entityMirror2;
    public final BlockRotation entityRotation;
    public final ImmutableList<OrderedTick<Block>> scheduledBlockTicks;
    public final ImmutableList<OrderedTick<Fluid>> scheduledFluidTicks;

    public UploadChunkPacket(ReplaceBehavior replaceBehavior, BlockPos minPos, BlockPos maxPos, BlockState[] blockData, List<NbtCompound> blockEntities, List<NbtCompound> entities, BlockMirror entityMirror1, BlockMirror entityMirror2, BlockRotation entityRotation, List<OrderedTick<Block>> scheduledBlockTicks, List<OrderedTick<Fluid>> scheduledFluidTicks) {
        this.replaceBehavior = replaceBehavior;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.blockData = blockData;
        this.blockEntities = blockEntities.toArray(new NbtCompound[0]);
        this.entities = entities.toArray(new NbtCompound[0]);
        this.entityMirror1 = entityMirror1;
        this.entityMirror2 = entityMirror2;
        this.entityRotation = entityRotation;
        this.scheduledBlockTicks = ImmutableList.copyOf(scheduledBlockTicks);
        this.scheduledFluidTicks = ImmutableList.copyOf(scheduledFluidTicks);
    }

    public UploadChunkPacket(PacketByteBuf buf) {
        this.replaceBehavior = buf.readEnumConstant(ReplaceBehavior.class);
        this.minPos = buf.readBlockPos();
        this.maxPos = buf.readBlockPos();

        List<BlockState> palette = buf.readList(UploadChunkPacket::readBlockState);
        if (palette.isEmpty()) {
            throw new DecoderException("upload_chunk block palette is empty");
        }

        int numBlocks = (Math.abs((maxPos.getX() & 15) - (minPos.getX() & 15)) + 1) * (Math.abs(maxPos.getY() - minPos.getY()) + 1) * (Math.abs((maxPos.getZ() & 15) - (minPos.getZ() & 15)) + 1);
        int bitsPerBlock = Math.max(1, MathHelper.ceilLog2(palette.size()));
        int numBits = numBlocks * bitsPerBlock;
        int numLongs = (numBits + 63) >>> 6;
        long[] data = buf.readLongArray(null, numLongs);
        if (data.length != numLongs) {
            throw new DecoderException("upload_chunk block data is wrong length");
        }
        long mask = (1L << bitsPerBlock) - 1;

        this.blockData = new BlockState[numBlocks];
        for (int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            int bitIndex = blockIndex * bitsPerBlock;
            int longIndex = bitIndex >>> 6;
            int endLongIndex = (bitIndex + bitsPerBlock - 1) >>> 6;
            int indexInLong = bitIndex & 63;
            int value;
            if (longIndex == endLongIndex) {
                value = (int) ((data[longIndex] >>> indexInLong) & mask);
            } else {
                int bitsInNextLong = indexInLong + bitsPerBlock - 64;
                value = (int) ((data[longIndex] >>> indexInLong) | ((data[endLongIndex] & ((1L << bitsInNextLong) - 1)) << (bitsPerBlock - bitsInNextLong)));
            }
            if (value >= palette.size()) {
                throw new DecoderException("upload_chunk block data contained block not in palette");
            }
            this.blockData[blockIndex] = palette.get(value);
        }

        this.blockEntities = new NbtCompound[buf.readVarInt()];
        for (int i = 0; i < this.blockEntities.length; i++) {
            this.blockEntities[i] = buf.readNbt();
        }

        this.entities = new NbtCompound[buf.readVarInt()];
        for (int i = 0; i < this.entities.length; i++) {
            this.entities[i] = buf.readNbt();
        }

        this.entityMirror1 = buf.readEnumConstant(BlockMirror.class);
        this.entityMirror2 = buf.readEnumConstant(BlockMirror.class);
        this.entityRotation = buf.readEnumConstant(BlockRotation.class);

        this.scheduledBlockTicks = readOrderedTickList(Registry.BLOCK, buf);
        this.scheduledFluidTicks = readOrderedTickList(Registry.FLUID, buf);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(replaceBehavior);
        buf.writeBlockPos(minPos);
        buf.writeBlockPos(maxPos);

        List<BlockState> palette = new ArrayList<>();
        Object2IntMap<BlockState> paletteIndexes = new Object2IntOpenCustomHashMap<>(Util.identityHashStrategy());
        for (BlockState state : blockData) {
            paletteIndexes.computeIfAbsent(state, k -> {
                int index = palette.size();
                palette.add(state);
                return index;
            });
        }

        buf.writeCollection(palette, UploadChunkPacket::writeBlockState);
        int bitsPerBlock = Math.max(1, MathHelper.ceilLog2(palette.size()));
        int numBits = blockData.length * bitsPerBlock;
        long[] data = new long[(numBits + 63) >>> 6];
        for (int blockIndex = 0; blockIndex < blockData.length; blockIndex++) {
            int bitIndex = blockIndex * bitsPerBlock;
            int longIndex = bitIndex >>> 6;
            int endLongIndex = (bitIndex + bitsPerBlock - 1) >>> 6;
            int indexInLong = bitIndex & 63;
            int value = paletteIndexes.getInt(blockData[blockIndex]);
            data[longIndex] |= (long) value << indexInLong;
            if (longIndex != endLongIndex) {
                int bitsAlreadyWritten = 64 - indexInLong;
                data[endLongIndex] = (long) value >>> bitsAlreadyWritten;
            }
        }
        buf.writeLongArray(data);

        buf.writeVarInt(blockEntities.length);
        for (NbtCompound blockEntity : blockEntities) {
            buf.writeNbt(blockEntity);
        }

        buf.writeVarInt(entities.length);
        for (NbtCompound entity : entities) {
            buf.writeNbt(entity);
        }

        buf.writeEnumConstant(entityMirror1);
        buf.writeEnumConstant(entityMirror2);
        buf.writeEnumConstant(entityRotation);

        buf.writeCollection(scheduledBlockTicks, (buf1, orderedTick) -> writeOrderedTick(Registry.BLOCK, buf1, orderedTick));
        buf.writeCollection(scheduledFluidTicks, (buf1, orderedTick) -> writeOrderedTick(Registry.FLUID, buf1, orderedTick));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState readBlockState(PacketByteBuf buf) {
        Identifier blockId = buf.readIdentifier();
        Block block = Registry.BLOCK.get(blockId);
        BlockState state = block.getDefaultState();
        int numProperties = buf.readVarInt();
        for (int i = 0; i < numProperties; i++) {
            String propertyName = buf.readString(256);
            Property<T> property = (Property<T>) block.getStateManager().getProperty(propertyName);
            if (property == null) {
                continue;
            }
            String propertyValue = buf.readString(256);
            Optional<T> value = property.parse(propertyValue);
            if (value.isEmpty()) {
                continue;
            }
            state = state.with(property, value.get());
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> void writeBlockState(PacketByteBuf buf, BlockState state) {
        Identifier blockId = Registry.BLOCK.getId(state.getBlock());
        buf.writeIdentifier(blockId);
        ImmutableMap<Property<?>, Comparable<?>> entries = state.getEntries();
        buf.writeVarInt(entries.size());
        entries.forEach((p, v) -> {
            Property<T> property = (Property<T>) p;
            T value = (T) v;
            buf.writeString(property.getName(), 256);
            buf.writeString(property.name(value), 256);
        });
    }

    private static <T> ImmutableList<OrderedTick<T>> readOrderedTickList(Registry<T> registry, PacketByteBuf buf) {
        int size = buf.readVarInt();
        ImmutableList.Builder<OrderedTick<T>> builder = ImmutableList.builderWithExpectedSize(size);
        for (int i = 0; i < size; i++) {
            builder.add(readOrderedTick(registry, buf));
        }
        return builder.build();
    }

    private static <T> OrderedTick<T> readOrderedTick(Registry<T> registry, PacketByteBuf buf) {
        Identifier id = buf.readIdentifier();
        T value = registry.get(id);
        BlockPos pos = buf.readBlockPos();
        long triggerTick = buf.readVarLong();
        TickPriority priority = TickPriority.byIndex(buf.readUnsignedByte() - 3);
        long subTickOrder = buf.readVarLong();
        return new OrderedTick<>(value, pos, triggerTick, priority, subTickOrder);
    }

    private static <T> void writeOrderedTick(Registry<T> registry, PacketByteBuf buf, OrderedTick<T> orderedTick) {
        buf.writeIdentifier(registry.getId(orderedTick.type()));
        buf.writeBlockPos(orderedTick.pos());
        buf.writeVarLong(orderedTick.triggerTick());
        buf.writeByte(orderedTick.priority().getIndex() + 3);
        buf.writeVarLong(orderedTick.subTickOrder());
    }

    @Override
    public PacketSplitter.SplitPacketType<?> getType() {
        return TYPE;
    }

    public enum ReplaceBehavior {
        NONE, ALL, WITH_NON_AIR
    }
}
