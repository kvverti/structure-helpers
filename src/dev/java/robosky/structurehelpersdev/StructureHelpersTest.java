package robosky.structurehelpersdev;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.structure.pool.ElementRange;
import robosky.structurehelpers.structure.pool.ExtendedSinglePoolElement;
import robosky.structurehelpers.structure.pool.ExtendedStructureFeature;
import robosky.structurehelpers.structure.pool.ExtendedStructurePoolFeatureConfig;
import robosky.structurehelpers.structure.processor.AirGroundReplacementProcessor;
import robosky.structurehelpers.structure.processor.PartialBlockState;
import robosky.structurehelpers.structure.processor.WeightedChanceProcessor;
import robosky.structurehelpers.structure.processor.WeightedChanceProcessor.Entry;
import robosky.structurehelpersdev.mixin.StructureFeatureAccess;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.TemplatePools;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;

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
                () -> TestStructureFeature.START,
                16));
        BuiltinRegistries.add(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE, StructureHelpers.id("test_dungeon"), configuredFeature);
    }
}

class TestStructureFeature extends ExtendedStructureFeature {

    static Identifier id(String s) {
        return new Identifier("tut", s);
    }

    public static final StructurePool START;

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
        START = TemplatePools.register(
            new StructurePool(
                id("start"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        TemplatePools.register(
            new StructurePool(
                id("halls"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("stairs"), true, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("corridor"), true, ls), 6),
                    Pair.of(ExtendedSinglePoolElement.of(id("chest_corridor"), true, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("prison"), true, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("corner"), true, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), true, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        TemplatePools.register(
            new StructurePool(
                id("halls_and_rooms"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("stairs"), true, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("corridor"), true, ls), 6),
                    Pair.of(ExtendedSinglePoolElement.of(id("chest_corridor"), true, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("prison"), true, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("corner"), true, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), true, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), true, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("room"), true, ls), 7),
                    Pair.of(ExtendedSinglePoolElement.of(id("end_portal"), true, endLs), 1)
                ),
                Projection.RIGID
            )
        );
        TemplatePools.register(
            new StructurePool(
                id("stairway-term"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        TemplatePools.register(
            new StructurePool(
                id("stairway"),
                id("stairway-term"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral"), true, ls), 100)
//                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), true, ls), 1),
//                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), true, ls), 1)
                ),
                Projection.RIGID
            )
        );
        TemplatePools.register(
            new StructurePool(
                id("doors"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("wooden_door"), true, childLs), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("iron_door"), true, childLs), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("empty_door"), true, childLs), 1)
                ),
                Projection.RIGID
            )
        );
        TemplatePools.register(
            new StructurePool(
                id("deco"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("torch"), true, childLs), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("fountain"), true, childLs), 1)
                ),
                Projection.RIGID
            )
        );
    }

    public TestStructureFeature() {
        super(ExtendedStructurePoolFeatureConfig.CODEC, 30, false, false);
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
}
