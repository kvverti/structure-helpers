package robosky.structurehelpers.block;

import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;

public class LootDataBlockEntity extends BlockEntity implements BlockEntityClientSerializable {

    private Identifier lootTable = new Identifier("minecraft:empty");

    private String replacementState = "minecraft:air";

    public LootDataBlockEntity() {
        super(StructureHelpers.LOOT_DATA_ENTITY_TYPE);
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
    public CompoundTag toClientTag(CompoundTag tag) {
        tag.putString("LootTable", lootTable.toString());
        tag.putString("Replacement", replacementState);
        return tag;
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        return toClientTag(super.toTag(tag));
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        replacementState = tag.getString("Replacement");
        try {
            lootTable = new Identifier(tag.getString("LootTable"));
        } catch (InvalidIdentifierException e) {
            lootTable = new Identifier("minecraft:air");
        }
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        fromClientTag(tag);
    }
}
