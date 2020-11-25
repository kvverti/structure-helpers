package robosky.structurehelpers.structure.pool;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.mixin.StructurePlacementDataAccessor;
import robosky.structurehelpers.structure.LootDataUtil;

import net.minecraft.structure.Structure;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Single pool element with structure block support and other
 * extended capabilities.
 */
public class ExtendedSinglePoolElement extends SinglePoolElement {

    public static final Codec<ExtendedSinglePoolElement> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        SinglePoolElement.method_28882(),
        SinglePoolElement.method_28880(),
        StructurePoolElement.method_28883(),
        Codec.BOOL.fieldOf("OverwriteFluids").forGetter(e -> e.overwriteFluids)
    ).apply(inst, ExtendedSinglePoolElement::new));

    public static final StructurePoolElementType<ExtendedSinglePoolElement> TYPE = () -> CODEC;

    /**
     * Whether blocks in this element should overwrite fluids. If false,
     * existing fluids will be merged with the structure's blocks.
     */
    private final boolean overwriteFluids;

    private ExtendedSinglePoolElement(
        Either<Identifier, Structure> location,
        Supplier<StructureProcessorList> processors,
        Projection projection,
        boolean overwriteFluids
    ) {
        super(location, processors, projection);
        this.overwriteFluids = overwriteFluids;
    }

    public static Function<StructurePool.Projection, ExtendedSinglePoolElement> of(Identifier location) {
        return of(location, false, ImmutableList.of());
    }

    public static Function<StructurePool.Projection, ExtendedSinglePoolElement> of(
        Identifier location,
        boolean overwriteFluids,
        ImmutableList<StructureProcessor> processors
    ) {
        return proj -> new ExtendedSinglePoolElement(Either.left(location), () -> new StructureProcessorList(processors), proj, overwriteFluids);
    }

    /**
     * The structure element ID.
     */
    public final Identifier location() {
        return this.field_24015.left()
            .orElseThrow(() -> new AssertionError("ExtendedSinglePoolElement created without ID"));
    }

    @Override
    public StructurePoolElementType<ExtendedSinglePoolElement> getType() {
        return TYPE;
    }

    // add/remove processors
    @Override
    protected StructurePlacementData createPlacementData(BlockRotation rot, BlockBox bbox, boolean b) {
        StructurePlacementData data = super.createPlacementData(rot, bbox, b);
        // allow air and structure blocks to work properly
        data.removeProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);
        ((StructurePlacementDataAccessor)data).setPlaceFluids(!overwriteFluids);
        return data;
    }

    @Override
    public boolean generate(
        StructureManager manager, StructureWorldAccess world, StructureAccessor accessor, ChunkGenerator generator,
        BlockPos pos, BlockPos pos2, BlockRotation rotation, BlockBox box, Random rand, boolean b
    ) {
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
