package robosky.structurehelpers.block;

import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;

public class LootDataBlockEntity extends BlockEntity {

    private Identifier lootTable = new Identifier("minecraft:empty");

    private String replacementState = "minecraft:air";

    public LootDataBlockEntity(BlockPos pos, BlockState state) {
        super(StructureHelpers.LOOT_DATA_ENTITY_TYPE, pos, state);
    }

    public Identifier getLootTable() {
        return lootTable;
    }

    public void setLootTable(Identifier lootTable) {
        this.lootTable = lootTable;
    }

    public String getReplacementState() {
        return replacementState;
    }

    public void setReplacementState(String replacementState) {
        this.replacementState = replacementState;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag = super.writeNbt(tag);
        tag.putString("LootTable", lootTable.toString());
        tag.putString("Replacement", replacementState);
        return tag;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        replacementState = tag.getString("Replacement");
        try {
            lootTable = new Identifier(tag.getString("LootTable"));
        } catch(InvalidIdentifierException e) {
            lootTable = new Identifier("minecraft:air");
        }
    }
}
