package robosky.structurehelpersdev;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.structure.ExtendedStructures;
import robosky.structurehelpers.structure.piece.ExtendedStructurePiece;
import robosky.structurehelpers.structure.pool.ElementRange;
import robosky.structurehelpers.structure.pool.ExtendedSinglePoolElement;
import robosky.structurehelpers.structure.processor.PartialBlockState;
import robosky.structurehelpers.structure.processor.WeightedChanceProcessor;
import robosky.structurehelpers.structure.processor.WeightedChanceProcessor.Entry;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.NopeDecoratorConfig;
import net.minecraft.world.gen.feature.AbstractTempleFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

import net.fabricmc.api.ModInitializer;

public class StructureHelpersTest implements ModInitializer {

    @Override
    public void onInitialize() {
        TestStructureFeature feature = Registry.register(Registry.FEATURE,
            StructureHelpers.id("test_dungeon"),
            new TestStructureFeature());
        Registry.register(Registry.STRUCTURE_FEATURE, StructureHelpers.id("test_dungeon"), feature);
        Feature.STRUCTURES.put(feature.getName(), feature);
        for(Biome biome : Registry.BIOME) {
            biome.addStructureFeature(feature.configure(FeatureConfig.DEFAULT));
            biome.addFeature(GenerationStep.Feature.UNDERGROUND_STRUCTURES,
                feature.configure(DefaultFeatureConfig.DEFAULT)
                    .createDecoratedFeature(Decorator.NOPE.configure(NopeDecoratorConfig.DEFAULT)));
        }
    }
}

class TestStructureFeature extends AbstractTempleFeature<DefaultFeatureConfig> {

    public static final ExtendedStructurePiece.Factory TYPE = Registry.register(
        Registry.STRUCTURE_PIECE,
        StructureHelpers.id("test"),
        ExtendedStructurePiece.newFactory()
    );

    private static Identifier id(String s) {
        return new Identifier("tut", s);
    }

    static {
        ImmutableList<StructureProcessor> ls = ImmutableList.of(
            WeightedChanceProcessor.builder()
                .add(Blocks.STONE_BRICKS,
                    Entry.of(Blocks.STONE_BRICKS, 0.6f),
                    Entry.of(Blocks.CRACKED_STONE_BRICKS, 0.2f),
                    Entry.of(Blocks.MOSSY_STONE_BRICKS, 0.2f))
                .build()
        );
        WeightedChanceProcessor.Builder builder = WeightedChanceProcessor.builder();
        builder.add(Blocks.END_PORTAL_FRAME,
            Entry.of(PartialBlockState.builder(Blocks.END_PORTAL_FRAME).with(Properties.EYE, true).build(), 0.5f),
            Entry.of(PartialBlockState.builder(Blocks.END_PORTAL_FRAME).with(Properties.EYE, false).build(), 0.5f)
        );
        ImmutableList<StructureProcessor> endLs = ImmutableList.<StructureProcessor>builder()
            .addAll(ls)
            .add(builder.build())
            .build();
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("start"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("spiral_bottom"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("halls"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("stairs"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("corridor"), true, ls), 6),
                    Pair.of(new ExtendedSinglePoolElement(id("chest_corridor"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("prison"), true, ls), 2),
                    Pair.of(new ExtendedSinglePoolElement(id("corner"), true, ls), 2),
                    Pair.of(new ExtendedSinglePoolElement(id("spiral_top"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("spiral_bottom"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("halls_and_rooms"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("stairs"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("corridor"), true, ls), 6),
                    Pair.of(new ExtendedSinglePoolElement(id("chest_corridor"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("prison"), true, ls), 2),
                    Pair.of(new ExtendedSinglePoolElement(id("corner"), true, ls), 2),
                    Pair.of(new ExtendedSinglePoolElement(id("spiral_top"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("spiral_bottom"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("room"), true, ls), 7)
//                    Pair.of(new ExtendedSinglePoolElement(id("end_portal"), true, endLs), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("stairway-term"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("spiral_top"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("stairway"),
                id("stairway-term"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("spiral"), true, ls), 100)
//                    Pair.of(new ExtendedSinglePoolElement(id("spiral_top"), true, ls), 1),
//                    Pair.of(new ExtendedSinglePoolElement(id("spiral_bottom"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("doors"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("wooden_door"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("iron_door"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("empty_door"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("deco"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("torch"), true, ls), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("fountain"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
    }

    public TestStructureFeature() {
        super(DefaultFeatureConfig::deserialize);
    }

    @Override
    protected int getSeedModifier(ChunkGeneratorConfig config) {
        return 0;
    }

    @Override
    public StructureStartFactory getStructureStartFactory() {
        return Start::new;
    }

    @Override
    public String getName() {
        return StructureHelpers.id("structure_helpers_test").toString();
    }

    @Override
    public int getRadius() {
        return 8;
    }

    private static class Start extends StructureStart {

        public Start(
            StructureFeature<?> feature,
            int chunkX,
            int chunkZ,
            BlockBox box,
            int references,
            long seed
        ) {
            super(feature, chunkX, chunkZ, box, references, seed);
        }

        @Override
        public void init(
            ChunkGenerator<?> generator, StructureManager manager, int chunkX, int chunkZ, Biome biome
        ) {
            List<PoolStructurePiece> pieces = ExtendedStructures.addPieces(
                ImmutableList.of(ElementRange.of(id("end_portal"), 1, 1)),
                0,
                256,
                id("start"),
                32,
                TYPE,
                generator,
                manager,
                new BlockPos(chunkX * 16, 100, chunkZ * 16),
                this.random,
                false, // don't know what this does yet
                false);
            this.children.addAll(pieces);
            this.setBoundingBoxFromChildren();
        }
    }
}
