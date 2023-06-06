package net.earthcomputer.litemoretica.mixin.client;

import net.earthcomputer.litemoretica.network.PacketSplitter;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin_PacketSplitter implements PacketSplitter.NetHandlerExt {
    @Unique
    private final PacketSplitter litemoretica_packetSplitter = new PacketSplitter();

    @Override
    public PacketSplitter litemoretica_getPacketSplitter() {
        return litemoretica_packetSplitter;
    }
}
