package robosky.structurehelpers.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.TranslatableText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.iface.StructHelpServerPacketListener;
import robosky.structurehelpers.network.UpdateLootDataC2SPacket;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements StructHelpServerPacketListener {

    @Shadow
    public ServerPlayerEntity player;

    @Override
    public void structhelp_onLootData(UpdateLootDataC2SPacket packet) {
        ServerWorld world = player.getServerWorld();
        NetworkThreadUtils.forceMainThread(packet, this, world);
        BlockEntity be = world.getBlockEntity(packet.getPos());
        if(be instanceof LootDataBlockEntity) {
            LootDataBlockEntity ld = (LootDataBlockEntity)be;
            ld.setLootTable(packet.getLootTable());
            ld.setReplacementState(packet.getReplacement());
            this.player.sendMessage(new TranslatableText("structure-helpers.updated_loot", packet.getLootTable()));
        }
    }
}
