package robosky.structurehelpers.network;

import java.io.IOException;

import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import robosky.structurehelpers.iface.StructHelpServerPacketListener;

/**
 * Sends client data from the loot data GUI to the server.
 */
public final class UpdateLootDataC2SPacket implements Packet<StructHelpServerPacketListener> {

    private final LootDataPacketData data;

    public UpdateLootDataC2SPacket() {
        this(BlockPos.ORIGIN, "minecraft:empty", "minecraft:air");
    }

    public UpdateLootDataC2SPacket(BlockPos pos, String lootTable, String replacement) {
        this.data = new LootDataPacketData(pos, lootTable, replacement);
    }

    public BlockPos getPos() {
    	return data.getPos();
    }

    public Identifier getLootTable() {
        return data.getLootTable();
    }

    public String getReplacement() {
    	return data.getReplacement();
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        data.read(buf);
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        data.write(buf);
    }

    @Override
    public void apply(StructHelpServerPacketListener listener) {
        listener.structhelp_onLootData(this);
    }
}
