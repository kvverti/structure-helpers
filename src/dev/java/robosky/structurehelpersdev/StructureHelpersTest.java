package robosky.structurehelpersdev;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.structure.ExtendedStructures;
import robosky.structurehelpers.structure.piece.ExtendedStructurePiece;
import robosky.structurehelpers.structure.pool.ElementRange;
import robosky.structurehelpers.structure.pool.ExtendedSinglePoolElement;
import robosky.structurehelpers.structure.pool.ExtendedStructurePoolFeatureConfig;
import robosky.structurehelpers.structure.processor.AirGroundReplacementProcessor;
import robosky.structurehelpers.structure.processor.PartialBlockState;
import robosky.structurehelpers.structure.processor.WeightedChanceProcessor;
import robosky.structurehelpers.structure.processor.WeightedChanceProcessor.Entry;
import robosky.structurehelpersdev.mixin.StructureFeatureAccess;

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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;

import net.fabricmc.api.ModInitializer;

public class StructureHelpersTest implements ModInitializer {

    @Override
    public void onInitialize() {
        TestStructureFeature feature = StructureFeatureAccess.callRegister(
            StructureHelpers.id("test_dungeon").toString(),
            new TestStructureFeature(),
            GenerationStep.Feature.UNDERGROUND_STRUCTURES);
        ConfiguredStructureFeature<?, ?> configuredFeature = feature.configure(
            new ExtendedStructurePoolFeatureConfig(
                ImmutableList.of(ElementRange.of(TestStructureFeature.id("end_portal"), 1, 1)),
                0,
                256,
                TestStructureFeature.id("start"), 16));
        for(Biome biome : Registry.BIOME) {
            biome.addStructureFeature(configuredFeature);
        }
    }
}

class TestStructureFeature extends StructureFeature<ExtendedStructurePoolFeatureConfig> {

    public static final ExtendedStructurePiece.Factory TYPE = Registry.register(
        Registry.STRUCTURE_PIECE,
        StructureHelpers.id("test"),
        ExtendedStructurePiece.newFactory()
    );

    static Identifier id(String s) {
        return new Identifier("tut", s);
    }

    static {
        WeightedChanceProcessor stoneDecor = WeightedChanceProcessor.builder()
            .add(Blocks.STONE_BRICKS,
                Entry.of(Blocks.STONE_BRICKS, 0.6f),
                Entry.of(Blocks.CRACKED_STONE_BRICKS, 0.2f),
                Entry.of(Blocks.MOSSY_STONE_BRICKS, 0.2f))
            .build();
        AirGroundReplacementProcessor decay =
            AirGroundReplacementProcessor.create(AirGroundReplacementProcessor.Entry.groundOnly(Blocks.STONE_BRICKS));
        ImmutableList<StructureProcessor> ls = ImmutableList.of(decay, stoneDecor);
        ImmutableList<StructureProcessor> childLs = ImmutableList.of(stoneDecor);
        ImmutableList<StructureProcessor> endLs = ImmutableList.of(stoneDecor,
            WeightedChanceProcessor.builder().add(Blocks.END_PORTAL_FRAME,
                Entry.of(PartialBlockState.builder(Blocks.END_PORTAL_FRAME).with(Properties.EYE, true).build(), 0.5f),
                Entry.of(PartialBlockState.builder(Blocks.END_PORTAL_FRAME).with(Properties.EYE, false).build(), 0.5f)
            ).build());
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
                    Pair.of(new ExtendedSinglePoolElement(id("room"), true, ls), 7),
                    Pair.of(new ExtendedSinglePoolElement(id("end_portal"), true, endLs), 1)
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
                    Pair.of(new ExtendedSinglePoolElement(id("wooden_door"), true, childLs), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("iron_door"), true, childLs), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("empty_door"), true, childLs), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePoolBasedGenerator.REGISTRY.add(
            new StructurePool(
                id("deco"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(new ExtendedSinglePoolElement(id("torch"), true, childLs), 1),
                    Pair.of(new ExtendedSinglePoolElement(id("fountain"), true, childLs), 1)
                ),
                Projection.RIGID
            )
        );
    }

    public TestStructureFeature() {
        super(ExtendedStructurePoolFeatureConfig.CODEC);
    }

    @Override
    public StructureStartFactory<ExtendedStructurePoolFeatureConfig> getStructureStartFactory() {
        return Start::new;
    }

    @Override
    protected boolean shouldStartAt(
        ChunkGenerator chunkGenerator,
        BiomeSource biomeSource,
        long l,
        ChunkRandom chunkRandom,
        int i,
        int j,
        Biome biome,
        ChunkPos chunkPos,
        ExtendedStructurePoolFeatureConfig featureConfig
    ) {
        return i % 8 == 0 && j % 8 == 0 && chunkRandom.nextInt(5) == 0;
    }

    private static class Start extends StructureStart<ExtendedStructurePoolFeatureConfig> {

        public Start(
            StructureFeature<ExtendedStructurePoolFeatureConfig> feature,
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
            ChunkGenerator generator,
            StructureManager manager,
            int chunkX,
            int chunkZ,
            Biome biome,
            ExtendedStructurePoolFeatureConfig config
        ) {
            List<PoolStructurePiece> pieces = ExtendedStructures.addPieces(
                config.rangeConstraints,
                config.horizontalExtent,
                config.verticalExtent,
                config.startPool,
                config.size,
                TYPE,
                generator,
                manager,
                new BlockPos(chunkX * 16, 30, chunkZ * 16),
                this.random,
                false, // don't know what this does yet
                false);
            this.children.addAll(pieces);
            this.setBoundingBoxFromChildren();
        }
    }
}
