package robosky.structurehelpers.network;

import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.block.LootDataBlockEntity;

public final class ServerStructHelpPackets {

    private ServerStructHelpPackets() {}

    public static final Identifier LOOT_DATA_UPDATE =
        new Identifier(StructureHelpers.MODID, "loot_data_update");

    /**
     * Updates loot data sent from the client to the server.
     */
    private static void updateLootData(PacketContext ctx, PacketByteBuf buf) {
        LootDataPacketData data = new LootDataPacketData();
        data.read(buf);
        PlayerEntity player = ctx.getPlayer();
        ctx.getTaskQueue().execute(() -> {
            BlockEntity be = player.getEntityWorld().getBlockEntity(data.getPos());
            if (be instanceof LootDataBlockEntity && player.isCreativeLevelTwoOp()) {
                LootDataBlockEntity ld = (LootDataBlockEntity)be;
                ld.setLootTable(data.getLootTable());
                ld.setReplacementState(data.getReplacement());
                player.sendMessage(new TranslatableText("structure-helpers.updated_loot", data.getLootTable()));
            }
        });
    }

    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(LOOT_DATA_UPDATE, ServerStructHelpPackets::updateLootData);
    }
}
