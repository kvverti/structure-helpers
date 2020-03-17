package robosky.structurehelpers.network;

import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.iface.JigsawAccessorData;

public final class ServerStructHelpPackets {

    private ServerStructHelpPackets() {}

    public static final Identifier LOOT_DATA_UPDATE =
        new Identifier(StructureHelpers.MODID, "loot_data_update");
    public static final Identifier JIGSAW_OFFSET_UPDATE =
        new Identifier(StructureHelpers.MODID, "jigsaw_offset_update");

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

    /**
     * Updates the jigsaw offset state when a player sets it.
     */
    private static void updateJigsawOffset(PacketContext ctx, PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean childJunction = buf.readBoolean();
        PlayerEntity player = ctx.getPlayer();
        ctx.getTaskQueue().execute(() -> {
            BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
            if(be instanceof JigsawAccessorData) {
                ((JigsawAccessorData)be).structhelp_setChildJunction(childJunction);
            }
        });
    }

    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(LOOT_DATA_UPDATE, ServerStructHelpPackets::updateLootData);
        ServerSidePacketRegistry.INSTANCE.register(JIGSAW_OFFSET_UPDATE, ServerStructHelpPackets::updateJigsawOffset);
    }
}
