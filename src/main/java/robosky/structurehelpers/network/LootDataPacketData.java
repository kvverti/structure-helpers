package robosky.structurehelpers.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;

/**
 * Common loot data packets data
 */
public final class LootDataPacketData {

    private BlockPos pos;

    private Identifier lootTable;

    private String replacement;

    public LootDataPacketData() {
        this(BlockPos.ORIGIN, "minecraft:empty", "minecraft:air");
    }

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

    public void read(PacketByteBuf buf) {
        pos = buf.readBlockPos();
        try {
            lootTable = new Identifier(buf.readString());
        } catch(InvalidIdentifierException e) {
            lootTable = new Identifier("minecraft:empty");
        }
        replacement = buf.readString();
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(lootTable.toString());
        buf.writeString(replacement);
    }
}
