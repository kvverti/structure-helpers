package robosky.structurehelpers.block;

import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;

public final class SpawnDataBlockEntity extends BlockEntity implements BlockEntityClientSerializable {

    private EntityType<?> entityType = EntityType.PIG;
    private CompoundTag parameters = new CompoundTag();

    public SpawnDataBlockEntity() {
        super(StructureHelpers.SPAWN_DATA_ENTITY_TYPE);
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    public CompoundTag getParameters() {
        return parameters;
    }

    public void setParameters(CompoundTag parameters) {
        this.parameters = parameters;
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        Identifier entityId = Identifier.tryParse(tag.getString("EntityType"));
        if(entityId != null) {
            entityType = Registry.ENTITY_TYPE.get(entityId);
        }
        if(entityType == null) {
            entityType = EntityType.PIG;
        }
        parameters = tag.getCompound("Parameters");
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        tag.putString("EntityType", Registry.ENTITY_TYPE.getId(entityType).toString());
        tag.put("Parameters", parameters);
        return tag;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        fromClientTag(tag);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        return toClientTag(super.toTag(tag));
    }
}
