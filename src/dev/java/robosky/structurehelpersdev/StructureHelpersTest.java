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

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.structure.v1.FabricStructureBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;

public class StructureHelpersTest implements ModInitializer {

    @Override
    public void onInitialize() {
        // create and register structure feature
        TestStructureFeature feature = FabricStructureBuilder
            .create(StructureHelpers.id("test_dungeon"), new TestStructureFeature())
            .step(GenerationStep.Feature.UNDERGROUND_STRUCTURES)
            .defaultConfig(8, 4, 1)
            .register();
        Test2StructureFeature feature2 = FabricStructureBuilder
            .create(StructureHelpers.id("test_box"), new Test2StructureFeature())
            .step(GenerationStep.Feature.UNDERGROUND_STRUCTURES)
            .defaultConfig(8, 4, 17)
            .register();
        // create and register configured feature (may also be done via datapacks for datapack biomes)
        // sorry for the PascalCase names, they were made before I knew they were going to go into
        // datapack JSON objects
        ConfiguredStructureFeature<?, ?> configuredFeature = feature.configure(
            new ExtendedStructurePoolFeatureConfig(
                ImmutableList.of(ElementRange.of(TestStructureFeature.id("end_portal"), 1, 1)),
                0,
                256,
                () -> TestStructureFeature.START,
                16));
        ConfiguredStructureFeature<?, ?> configuredFeature2 = feature2.configure(
            new ExtendedStructurePoolFeatureConfig(
                ImmutableList.of(),
                0,
                0,
                () -> Test2StructureFeature.START,
                16));
        BuiltinRegistries.add(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE, StructureHelpers.id("test_dungeon"), configuredFeature);
        BuiltinRegistries.add(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE, StructureHelpers.id("test_box"), configuredFeature2);
        for(Biome biome : BuiltinRegistries.BIOME) {
            // only for biomes you want your structure to appear in
            biome.getGenerationSettings().getStructureFeatures().add(() -> configuredFeature);
            biome.getGenerationSettings().getStructureFeatures().add(() -> configuredFeature2);
        }
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
            AirGroundReplacementProcessor.create(
                AirGroundReplacementProcessor.Entry.groundOnly(Blocks.STONE_BRICKS),
                AirGroundReplacementProcessor.Entry.groundOnly(Blocks.AIR),
                AirGroundReplacementProcessor.Entry.groundOnly(Blocks.CAVE_AIR)
            );
        ImmutableList<StructureProcessor> ls = ImmutableList.of(decay, stoneDecor);
        ImmutableList<StructureProcessor> childLs = ImmutableList.of(stoneDecor);
        ImmutableList<StructureProcessor> endLs = ImmutableList.of(stoneDecor,
            WeightedChanceProcessor.builder().add(Blocks.END_PORTAL_FRAME,
                Entry.of(PartialBlockState.builder(Blocks.END_PORTAL_FRAME).with(Properties.EYE, true).build(), 0.5f),
                Entry.of(PartialBlockState.builder(Blocks.END_PORTAL_FRAME).with(Properties.EYE, false).build(), 0.5f)
            ).build());
        START = StructurePools.register(
            new StructurePool(
                id("start"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), false, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePools.register(
            new StructurePool(
                id("halls"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("stairs"), false, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("corridor"), false, ls), 6),
                    Pair.of(ExtendedSinglePoolElement.of(id("chest_corridor"), false, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("prison"), false, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("corner"), false, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), false, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), false, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePools.register(
            new StructurePool(
                id("halls_and_rooms"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("stairs"), false, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("corridor"), false, ls), 6),
                    Pair.of(ExtendedSinglePoolElement.of(id("chest_corridor"), false, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("prison"), false, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("corner"), false, ls), 2),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), false, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), false, ls), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("room"), false, ls), 7),
                    Pair.of(ExtendedSinglePoolElement.of(id("end_portal"), true, endLs), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePools.register(
            new StructurePool(
                id("stairway-term"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), false, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePools.register(
            new StructurePool(
                id("stairway"),
                id("stairway-term"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("spiral"), false, ls), 100)
//                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_top"), false, ls), 1),
//                    Pair.of(ExtendedSinglePoolElement.of(id("spiral_bottom"), false, ls), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePools.register(
            new StructurePool(
                id("doors"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("wooden_door"), false, childLs), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("iron_door"), false, childLs), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("empty_door"), false, childLs), 1)
                ),
                Projection.RIGID
            )
        );
        StructurePools.register(
            new StructurePool(
                id("deco"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(id("torch"), false, childLs), 1),
                    Pair.of(ExtendedSinglePoolElement.of(id("fountain"), false, childLs), 1)
                ),
                Projection.RIGID
            )
        );
    }

    public TestStructureFeature() {
        super(30, false, false);
    }
}

class Test2StructureFeature extends ExtendedStructureFeature {

    public static final StructurePool START = StructurePools.register(
            new StructurePool(
                TestStructureFeature.id("start_2"),
                new Identifier("empty"),
                ImmutableList.of(
                    Pair.of(ExtendedSinglePoolElement.of(TestStructureFeature.id("box")), 1)
                ),
                Projection.RIGID
            )
        );

    public Test2StructureFeature() {
        super(70, false, false);
    }
}
