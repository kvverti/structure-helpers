package robosky.structurehelpers.network;

import java.io.IOException;

import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import robosky.structurehelpers.iface.StructHelpServerPacketListener;

/**
 * Sends client data from the loot data GUI to the server.
 */
public final class UpdateLootDataC2SPacket implements Packet<StructHelpServerPacketListener> {

    private BlockPos pos;

    private Identifier lootTable;

    private String replacement;

    public UpdateLootDataC2SPacket() {
        this(BlockPos.ORIGIN, "minecraft:empty", "minecraft:air");
    }

    public UpdateLootDataC2SPacket(BlockPos pos, String lootTable, String replacement) {
        this.pos = pos;
        try {
            this.lootTable = new Identifier(lootTable);
        } catch(InvalidIdentifierException e) {
            this.lootTable = new Identifier("minecraft:empty");
        }
        this.replacement = replacement;
    }

    public BlockPos getPos() {
    	return pos;
    }

    public Identifier getLootTable() {
        return lootTable;
    }

    public String getReplacement() {
    	return replacement;
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        pos = buf.readBlockPos();
        try {
            lootTable = new Identifier(buf.readString());
        } catch(InvalidIdentifierException e) {
            lootTable = new Identifier("minecraft:empty");
        }
        replacement = buf.readString();
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        buf.writeBlockPos(pos);
        buf.writeString(lootTable.toString());
        buf.writeString(replacement);
    }

    @Override
    public void apply(StructHelpServerPacketListener listener) {
        listener.structhelp_onLootData(this);
    }
}
