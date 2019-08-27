package robosky.structurehelpers.block;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;

import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.block.entity.BlockEntity;

import net.minecraft.util.Identifier;

import robosky.structurehelpers.StructureHelpers;

public class LootDataBlockEntity extends BlockEntity implements BlockEntityClientSerializable {

    private Identifier lootTable = new Identifier("minecraft:empty");

    private String replacementState = "minecraft:air";

    public LootDataBlockEntity() {
        super(StructureHelpers.LOOT_DATA_ENTITY_TYPE);
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
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);
        fromClientTag(tag);
    }
}
