package net.earthcomputer.litemoretica.mixin.server;

import net.earthcomputer.litemoretica.network.PacketSplitter;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin_PacketSplitter implements PacketSplitter.NetHandlerExt {
    @Unique
    private final PacketSplitter litemoretica_packetSplitter = new PacketSplitter();

    @Override
    public PacketSplitter litemoretica_getPacketSplitter() {
        return litemoretica_packetSplitter;
    }
}
