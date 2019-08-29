package robosky.structurehelpers.network;

import java.io.IOException;

import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * Common data between the C2S and S2C loot data packets.
 */
final class LootDataPacketData {

    private BlockPos pos;

    private Identifier lootTable;

    private String replacement;

    public LootDataPacketData(BlockPos pos, String lootTable, String replacement) {
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

    public void read(PacketByteBuf buf) throws IOException {
        pos = buf.readBlockPos();
        try {
            lootTable = new Identifier(buf.readString());
        } catch(InvalidIdentifierException e) {
            lootTable = new Identifier("minecraft:empty");
        }
        replacement = buf.readString();
    }

    public void write(PacketByteBuf buf) throws IOException {
        buf.writeBlockPos(pos);
        buf.writeString(lootTable.toString());
        buf.writeString(replacement);
    }
}
