package net.earthcomputer.litemoretica.server;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.earthcomputer.litemoretica.network.InitEasyPlaceProtocolPacket;
import net.earthcomputer.litemoretica.network.SetEasyPlaceProtocolPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EasyPlaceProtocolServer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ImmutableSet<Property<?>> WHITELISTED_PROPERTIES = ImmutableSet.of(
        // BooleanProperty:
        // INVERTED - DaylightDetector
        // OPEN - Barrel, Door, FenceGate, Trapdoor
        // PERSISTENT - Leaves
        Properties.INVERTED,
        Properties.OPEN,
        Properties.PERSISTENT,
        // EnumProperty:
        // AXIS - Pillar
        // BLOCK_HALF - Stairs, Trapdoor
        // CHEST_TYPE - Chest
        // COMPARATOR_MODE - Comparator
        // DOOR_HINGE - Door
        // SLAB_TYPE - Slab - PARTIAL ONLY: TOP and BOTTOM, not DOUBLE
        // STAIR_SHAPE - Stairs (needed to get the correct state, otherwise the player facing would be a factor)
        // WALL_MOUNT_LOCATION - Button, Grindstone, Lever
        Properties.AXIS,
        Properties.BLOCK_HALF,
        Properties.CHEST_TYPE,
        Properties.COMPARATOR_MODE,
        Properties.DOOR_HINGE,
        Properties.SLAB_TYPE,
        Properties.STAIR_SHAPE,
        Properties.WALL_MOUNT_LOCATION,
        // IntProperty:
        // BITES - Cake
        // DELAY - Repeater
        // NOTE - NoteBlock
        // ROTATION - Banner, Sign, Skull
        Properties.BITES,
        Properties.DELAY,
        Properties.NOTE,
        Properties.ROTATION
    );

    private EasyPlaceProtocolServer() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler, InitEasyPlaceProtocolPacket.TYPE)) {
                // TODO: convert these packets to new API in 1.20
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                new InitEasyPlaceProtocolPacket(WHITELISTED_PROPERTIES).write(buf);
                sender.sendPacket(InitEasyPlaceProtocolPacket.TYPE, buf);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(SetEasyPlaceProtocolPacket.TYPE, (server, player, handler, buf, responseSender) -> {
            SetEasyPlaceProtocolPacket packet = new SetEasyPlaceProtocolPacket(buf);
            player.server.execute(() -> {
                LOGGER.info("Player {} is using easy place protocol {}", player.getEntityName(), packet.protocol());
                ((NetworkHandlerExt) player.networkHandler).litemoretica_setEasyPlaceProtocol(packet.protocol());
            });
        });
    }

    public static int getEasyPlaceProtocol(ServerPlayerEntity player) {
        return ((NetworkHandlerExt) player.networkHandler).litemoretica_getEasyPlaceProtocol();
    }

    public static <T extends Comparable<T>> BlockState applyEasyPlaceProtocolV3(BlockState state, ItemPlacementContext context) {
        int protocolValue = (int) (context.getHitPos().x - (double) context.getBlockPos().getX()) - 2;
        //System.out.printf("raw protocol value in: 0x%08X\n", protocolValue);

        if (protocolValue < 0) {
            return state;
        }

        @Nullable DirectionProperty property = getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property != null && property != Properties.VERTICAL_DIRECTION) {
            //System.out.printf("applying: 0x%08X\n", protocolValue);
            state = applyDirectionProperty(state, context, property, protocolValue);

            if (state == null) {
                return null;
            }

            // Consume the bits used for the facing
            protocolValue >>>= 3;
        }

        // Consume the lowest unused bit
        protocolValue >>>= 1;

        List<Property<?>> propList = new ArrayList<>(state.getBlock().getStateManager().getProperties());
        propList.sort(Comparator.comparing(Property::getName));

        try {
            for (Property<?> p : propList) {
                if (!(p instanceof DirectionProperty) && WHITELISTED_PROPERTIES.contains(p)) {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.floorLog2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;
                    //System.out.printf("trying to apply valInd: %d, bits: %d, prot val: 0x%08X\n", valueIndex, requiredBits, protocolValue);

                    if (valueIndex < list.size()) {
                        T value = list.get(valueIndex);

                        if (!state.get(prop).equals(value) && allowPropertyValueThroughProtocol(value)) {
                            //System.out.printf("applying %s: %s\n", prop.getName(), value);
                            state = state.with(prop, value);
                        }

                        protocolValue >>>= requiredBits;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Exception trying to apply placement protocol value", e);
        }

        return state;
    }

    private static BlockState applyDirectionProperty(BlockState state, ItemPlacementContext context,
                                                     DirectionProperty property, int protocolValue) {
        Direction facingOrig = state.get(property);
        Direction facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0xF) >> 1;

        if (decodedFacingIndex == 6) {
            // the opposite of the normal facing requested
            facing = facing.getOpposite();
        } else if (decodedFacingIndex <= 5) {
            facing = Direction.byId(decodedFacingIndex);

            if (!property.getValues().contains(facing)) {
                facing = context.getHorizontalPlayerFacing().getOpposite();
            }
        }

        //System.out.printf("plop facing: %s -> %s (raw: %d, dec: %d)\n", facingOrig, facing, rawFacingIndex, decodedFacingIndex);

        if (facing != facingOrig && property.getValues().contains(facing)) {
            if (state.getBlock() instanceof BedBlock) {
                BlockPos headPos = context.getBlockPos().offset(facing);

                if (!context.getWorld().getBlockState(headPos).canReplace(context)) {
                    return null;
                }
            }

            state = state.with(property, facing);
        }

        return state;
    }

    @Nullable
    public static DirectionProperty getFirstDirectionProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof DirectionProperty) {
                return (DirectionProperty) prop;
            }
        }

        return null;
    }

    private static boolean allowPropertyValueThroughProtocol(Comparable<?> value) {
        // don't allow duping slabs by forcing a double slab via the protocol
        return value != SlabType.DOUBLE;
    }
    
    public interface NetworkHandlerExt {
        int litemoretica_getEasyPlaceProtocol();
        void litemoretica_setEasyPlaceProtocol(int easyPlaceProtocol);
    }
}
