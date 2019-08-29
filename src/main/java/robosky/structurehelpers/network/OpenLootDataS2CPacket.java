package robosky.structurehelpers.network;

import net.minecraft.util.PacketByteBuf;
import java.io.IOException;
import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import robosky.structurehelpers.iface.StructHelpClientPacketListener;

/**
 * Synchronizes loot data on the server to the client before opening
 * the data block screen.
 */
public class OpenLootDataS2CPacket implements Packet<StructHelpClientPacketListener> {

    private final LootDataPacketData data;

    public OpenLootDataS2CPacket() {
        this(BlockPos.ORIGIN, "minecraft:empty", "minecraft:air");
    }

    public OpenLootDataS2CPacket(BlockPos pos, String lootTable, String replace) {
        data = new LootDataPacketData(pos, lootTable, replace);
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
    public void apply(StructHelpClientPacketListener listener) {
        listener.structhelp_onOpenLootData(this);
    }
}
