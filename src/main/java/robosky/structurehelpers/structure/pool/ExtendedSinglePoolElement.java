package robosky.structurehelpers.structure.pool;

import com.google.common.collect.ImmutableList;

import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;

import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.registry.Registry;

import robosky.structurehelpers.StructureHelpers;

/**
 * Single pool element with capabilities of the rotation control and
 * structure block support.
 */
public class ExtendedSinglePoolElement extends SinglePoolElement {

    public static final StructurePoolElementType TYPE =
        Registry.register(Registry.STRUCTURE_POOL_ELEMENT, StructureHelpers.id("metadata_element"), ExtendedSinglePoolElement::new);

    /**
     * The rotation behavior of an {@link ExtendedSinglePoolElement}.
     */
    public enum RotationType {
        /**
         * No rotation shall be applied to the pool element.
         */
        NONE,

        /**
         * The element may be rotated in the horizontal plane (vanilla default).
         */
        RANDOM,

        /**
         * The rotation will be inherited from the element which has
         * triggered the generation.
         */
        INHERITED
    }

    private final RotationType rotation;

    public ExtendedSinglePoolElement(Dynamic<?> dyn) {
        super(dyn);
        String str = dyn.get("rotation_type").asString("RANDOM");
        RotationType rotation;
        try {
            rotation = RotationType.valueOf(str);
        } catch(IllegalArgumentException e) {
            rotation = RotationType.RANDOM;
        }
        this.rotation = rotation;
    }
    
    public ExtendedSinglePoolElement(Identifier location, RotationType rotation) {
        this(location, rotation, ImmutableList.of());
    }

    public ExtendedSinglePoolElement(Identifier location, RotationType rotation, ImmutableList<StructureProcessor> processors) {
        super(location.toString(), processors, Projection.RIGID);
        this.rotation = rotation;
    }

    public final Identifier location() {
        return this.location;
    }

    public final RotationType rotationType() {
        return rotation;
    }

    // Serialization
    @Override
    public <T> Dynamic<T> rawToDynamic(DynamicOps<T> ops) {
        T value = super.rawToDynamic(ops).getValue();
        return new Dynamic<>(ops, ops.mergeInto(value, ops.createString("rotation_type"), ops.createString(rotation.name())));
    }

    @Override
    public StructurePoolElementType getType() {
       return TYPE;
    }

    // add/remove processors
    @Override
    protected StructurePlacementData createPlacementData(BlockRotation rot, BlockBox bbox) {
      StructurePlacementData data = super.createPlacementData(rot, bbox);
      // allow air and structure blocks to work properly
      data.removeProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);
      return data;
    }
}
