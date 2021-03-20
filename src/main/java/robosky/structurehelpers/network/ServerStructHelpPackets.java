package robosky.structurehelpers.network;

import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.block.StructureRepeaterBlockEntity;
import robosky.structurehelpers.iface.JigsawAccessorData;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ServerStructHelpPackets {

    private ServerStructHelpPackets() {
    }

    public static final Identifier LOOT_DATA_UPDATE =
        new Identifier(StructureHelpers.MODID, "loot_data_update");
    public static final Identifier JIGSAW_OFFSET_UPDATE =
        new Identifier(StructureHelpers.MODID, "jigsaw_offset_update");
    public static final Identifier REPEATER_UPDATE =
        new Identifier(StructureHelpers.MODID, "structure_repeater_update");

    /**
     * Updates loot data sent from the client to the server.
     */
    private static void updateLootData(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        LootDataPacketData data = new LootDataPacketData();
        data.read(buf);
        server.execute(() -> {
            BlockEntity be = player.getEntityWorld().getBlockEntity(data.getPos());
            if(be instanceof LootDataBlockEntity && player.isCreativeLevelTwoOp()) {
                LootDataBlockEntity ld = (LootDataBlockEntity)be;
                ld.setLootTable(data.getLootTable());
                ld.setReplacementState(data.getReplacement());
                ld.markDirty();
                player.sendMessage(new TranslatableText("structure-helpers.updated_loot", data.getLootTable()), false);
            }
        });
    }

    /**
     * Updates the jigsaw offset state when a player sets it.
     */
    private static void updateJigsawOffset(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        boolean childJunction = buf.readBoolean();
        server.execute(() -> {
            BlockEntity be = player.getEntityWorld().getBlockEntity(pos);
            if(be instanceof JigsawAccessorData) {
                ((JigsawAccessorData)be).structhelp_setChildJunction(childJunction);
                be.markDirty();
            }
        });
    }

    /**
     * Updates the structure repeater data sent from the client.
     */
    private static void updateRepeaterData(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        RepeaterPacketData data = new RepeaterPacketData();
        data.read(buf);
        server.execute(() -> {
            BlockEntity be = player.world.getBlockEntity(data.getPos());
            if(be instanceof StructureRepeaterBlockEntity && player.isCreativeLevelTwoOp()) {
                StructureRepeaterBlockEntity repeater = (StructureRepeaterBlockEntity)be;
                repeater.setMinRepeat(data.getMinRepeat());
                repeater.setMaxRepeat(data.getMaxRepeat());
                repeater.setStopAtSolid(data.isStopAtSolid());
                repeater.setModeData(data.getMode(), data.getModeState());
                repeater.markDirty();
            }
        });
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(LOOT_DATA_UPDATE, ServerStructHelpPackets::updateLootData);
        ServerPlayNetworking.registerGlobalReceiver(JIGSAW_OFFSET_UPDATE, ServerStructHelpPackets::updateJigsawOffset);
        ServerPlayNetworking.registerGlobalReceiver(REPEATER_UPDATE, ServerStructHelpPackets::updateRepeaterData);
    }
}
