package robosky.structurehelpers.structure.pool;

import com.mojang.serialization.Codec;

import net.minecraft.structure.MarginedStructureStart;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;

public class ExtendedStructureFeature extends StructureFeature<ExtendedStructurePoolFeatureConfig> {

    private final int height;
    private final boolean b;
    private final boolean generateAtSurface;

    public ExtendedStructureFeature(int height, boolean b, boolean generateAtSurface) {
        this(ExtendedStructurePoolFeatureConfig.CODEC, height, b, generateAtSurface);
    }

    /**
     * @deprecated Use {@link #ExtendedStructureFeature(int, boolean, boolean)} instead.
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    public ExtendedStructureFeature(Codec<ExtendedStructurePoolFeatureConfig> codec, int height, boolean b, boolean generateAtSurface) {
        super(codec);
        this.height = height;
        this.b = b;
        this.generateAtSurface = generateAtSurface;
    }

    @Override
    public StructureStartFactory<ExtendedStructurePoolFeatureConfig> getStructureStartFactory() {
        return (feature, chunkPos, referenceCount, worldSeed) ->
            new Start((ExtendedStructureFeature)feature, chunkPos, referenceCount, worldSeed);
    }

    public static class Start extends MarginedStructureStart<ExtendedStructurePoolFeatureConfig> {

        public Start(ExtendedStructureFeature structureFeature, ChunkPos chunkPos, int referenceCount, long worldSeed) {
            super(structureFeature, chunkPos, referenceCount, worldSeed);
        }

        @Override
        public void init(DynamicRegistryManager dynamicRegistryManager, ChunkGenerator chunkGenerator, StructureManager structureManager, ChunkPos chunkPos, Biome biome, ExtendedStructurePoolFeatureConfig featureConfig, HeightLimitView view) {
            ExtendedStructureFeature feature = (ExtendedStructureFeature)this.getFeature();
            BlockPos pos = new BlockPos(chunkPos.x * 16, feature.height, chunkPos.z * 16);
            StructurePoolBasedGenerator.generate(
                dynamicRegistryManager,
                featureConfig,
                PoolStructurePiece::new,
                chunkGenerator,
                structureManager,
                pos,
                this,
                this.random,
                feature.b,
                feature.generateAtSurface,
                view
            );
            this.setBoundingBoxFromChildren();
        }
    }
}
