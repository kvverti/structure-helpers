package robosky.structurehelpers.structure.pool;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Dynamic;
import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.structure.LootDataUtil;

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
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Single pool element with capabilities of the rotation control and
 * structure block support.
 */
public class ExtendedSinglePoolElement extends SinglePoolElement {

    public static final StructurePoolElementType TYPE =
        Registry.register(Registry.STRUCTURE_POOL_ELEMENT, StructureHelpers.id("metadata_element"), ExtendedSinglePoolElement::new);

    public ExtendedSinglePoolElement(Dynamic<?> dyn) {
        super(dyn);
    }

    public ExtendedSinglePoolElement(Identifier location) {
        this(location, ImmutableList.of());
    }

    public ExtendedSinglePoolElement(Identifier location, ImmutableList<StructureProcessor> processors) {
        super(location.toString(), processors, Projection.RIGID);
    }

    public final Identifier location() {
        return this.field_24015.left().orElseThrow(() -> new AssertionError("ExtendedSinglePoolElement created without ID"));
    }

    @Override
    public StructurePoolElementType getType() {
       return TYPE;
    }

    // add/remove processors
    @Override
    protected StructurePlacementData createPlacementData(BlockRotation rot, BlockBox bbox, boolean b) {
      StructurePlacementData data = super.createPlacementData(rot, bbox, b);
      // allow air and structure blocks to work properly
      data.removeProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);
      return data;
    }

    @Override
    public boolean generate(StructureManager manager, IWorld world, StructureAccessor accessor, ChunkGenerator<?> generator,
            BlockPos pos, BlockPos pos2, BlockRotation rotation, BlockBox box, Random rand, boolean b) {
        boolean ret = super.generate(manager, world, accessor, generator, pos, pos2, rotation, box, rand, b);
        // process loot data blocks
        if(ret) {
            List<StructureBlockInfo> ls = manager.getStructureOrBlank(location())
                .getInfosForBlock(pos, createPlacementData(rotation, box, b), StructureHelpers.LOOT_DATA_BLOCK);
            for(StructureBlockInfo info : ls) {
                LootDataUtil.handleLootData(world, info);
            }
        }
        return ret;
    }
}
