package robosky.structurehelpers.structure.pool;

import com.google.common.collect.ImmutableList;

import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;

import java.util.List;
import java.util.Random;

import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.structure.LootDataUtil;

/**
 * A single pool element that has extended capabilities, such as
 * rotation disabling and structure block support.
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
         * The element may be rotated about the vertical axis (vanilla default)
         */
        RANDOM,

        /**
         * The element's rotation will be inherited from the element that
         * triggered this element's generation.
         */
        INHERITED
    }

    private final RotationType rotation;

    public ExtendedSinglePoolElement(Dynamic<?> dyn) {
        super(dyn);
        String str = dyn.get("rotation_type").asString("RANDOM");
        RotationType typ;
        try {
            typ = RotationType.valueOf(str);
        } catch(IllegalArgumentException e) {
            typ = RotationType.RANDOM;
        }
        this.rotation = typ;
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

    // serialzation
    @Override
    public <T> Dynamic<T> method_16625(DynamicOps<T> ops) {
        T value = super.method_16625(ops).getValue();
        return new Dynamic<>(ops, ops.mergeInto(value, ops.createString("rotation_type"), ops.createString(rotation.name())));
    }

    @Override
    public StructurePoolElementType getType() {
       return TYPE;
    }

    // add/remove processors
    @Override
    protected StructurePlacementData method_16616(BlockRotation rot, BlockBox bbox) {
      StructurePlacementData data = super.method_16616(rot, bbox);
      // allow air and structure blocks to work properly
      data.removeProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);
      return data;
    }

    @Override
    public boolean generate(StructureManager manager, IWorld world, ChunkGenerator<?> generator,
            BlockPos pos, BlockRotation rotation, BlockBox box, Random rand) {
        boolean ret = super.generate(manager, world, generator, pos, rotation, box, rand);
        // process loot data blocks
        if(ret) {
            List<StructureBlockInfo> ls = manager.getStructureOrBlank(this.location)
                .method_16445(pos, method_16616(rotation, box), StructureHelpers.LOOT_DATA_BLOCK);
            for(StructureBlockInfo info : ls) {
                LootDataUtil.handleLootData(world, info);
            }
        }
        return ret;
    }
}
