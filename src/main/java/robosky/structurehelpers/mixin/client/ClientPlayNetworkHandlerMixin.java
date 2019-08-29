package robosky.structurehelpers.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.NetworkThreadUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.client.LootDataScreen;
import robosky.structurehelpers.iface.StructHelpClientPacketListener;
import robosky.structurehelpers.network.OpenLootDataS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements StructHelpClientPacketListener {

    @Shadow
    private MinecraftClient client;

    @Override
    public void structhelp_onOpenLootData(OpenLootDataS2CPacket packet) {
        NetworkThreadUtils.forceMainThread(packet, this, this.client);
        BlockEntity be = this.client.world.getBlockEntity(packet.getPos());
        if (be instanceof LootDataBlockEntity) {
            LootDataBlockEntity ld = (LootDataBlockEntity)be;
            ld.setLootTable(packet.getLootTable());
            ld.setReplacementState(packet.getReplacement());
            this.client.openScreen(new LootDataScreen(ld));
        }
    }
}
