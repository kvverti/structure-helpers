package robosky.structurehelpers.network;

import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.client.LootDataScreen;

public final class ClientStructHelpPackets {

    private ClientStructHelpPackets() {}

    public static final Identifier LOOT_DATA_OPEN =
        new Identifier(StructureHelpers.MODID, "loot_data_open");

    private static void openLootDataScreen(PacketContext ctx, PacketByteBuf buf) {
        LootDataPacketData data = new LootDataPacketData();
        data.read(buf);
        ctx.getTaskQueue().execute(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            BlockEntity be = client.world.getBlockEntity(data.getPos());
            if (be instanceof LootDataBlockEntity) {
                LootDataBlockEntity ld = (LootDataBlockEntity)be;
                ld.setLootTable(data.getLootTable());
                ld.setReplacementState(data.getReplacement());
                client.openScreen(new LootDataScreen(ld));
            }
        });
    }

    public static void init() {
        ClientSidePacketRegistry.INSTANCE.register(LOOT_DATA_OPEN, ClientStructHelpPackets::openLootDataScreen);
    }
}
