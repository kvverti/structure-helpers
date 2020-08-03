package robosky.structurehelpers.structure.pool;

import com.mojang.serialization.Codec;

import net.minecraft.structure.MarginedStructureStart;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;

public class ExtendedStructureFeature extends StructureFeature<ExtendedStructurePoolFeatureConfig> {

    private final int height;
    private final boolean b;
    private final boolean generateAtSurface;

    public ExtendedStructureFeature(Codec<ExtendedStructurePoolFeatureConfig> codec, int height, boolean b, boolean generateAtSurface) {
        super(codec);
        this.height = height;
        this.b = b;
        this.generateAtSurface = generateAtSurface;
    }

    @Override
    public StructureStartFactory<ExtendedStructurePoolFeatureConfig> getStructureStartFactory() {
        return (feature, chunkX, chunkZ, box, referenceCount, worldSeed) ->
            new Start((ExtendedStructureFeature)feature, chunkX, chunkZ, box, referenceCount, worldSeed);
    }

    public static class Start extends MarginedStructureStart<ExtendedStructurePoolFeatureConfig> {

        public Start(ExtendedStructureFeature structureFeature, int chunkX, int chunkZ, BlockBox box, int referenceCount, long worldSeed) {
            super(structureFeature, chunkX, chunkZ, box, referenceCount, worldSeed);
        }

        @Override
        public void init(DynamicRegistryManager dynamicRegistryManager, ChunkGenerator chunkGenerator, StructureManager structureManager, int chunkX, int chunkZ, Biome biome, ExtendedStructurePoolFeatureConfig featureConfig) {
            ExtendedStructureFeature feature = (ExtendedStructureFeature)this.getFeature();
            BlockPos pos = new BlockPos(chunkX * 16, feature.height, chunkZ * 16);
            StructurePoolBasedGenerator.method_30419(
                dynamicRegistryManager,
                featureConfig,
                PoolStructurePiece::new,
                chunkGenerator,
                structureManager,
                pos,
                this.children,
                this.random,
                feature.b,
                feature.generateAtSurface
            );
            this.setBoundingBoxFromChildren();
        }
    }
}
