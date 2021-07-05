package robosky.structurehelpers.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import robosky.structurehelpers.iface.StructurePoolGeneratorAddition;
import robosky.structurehelpers.structure.pool.ElementRange;
import robosky.structurehelpers.structure.pool.ExtendedStructurePoolFeatureConfig;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiecesHolder;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;

/**
 * Note: because this class is accessed from multiple threads, there is no mutable
 * static state added to this class. Instead, the state is passed into this class
 * via the {@code List} method parameter.
 */
@Mixin(StructurePoolBasedGenerator.class)
abstract class StructurePoolBasedGeneratorOuterMixin {

    @Unique
    private static final ThreadLocal<ExtendedStructurePoolFeatureConfig> featureConfig = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<StructurePoolGeneratorAddition> poolGenerator = new ThreadLocal<>();

    /**
     * Extract element placement ranges from the child element list
     * out parameter.
     */
    @Inject(method = "generate", at = @At("HEAD"))
    private static void extractRoomMinMax(
        DynamicRegistryManager registryManager,
        StructurePoolFeatureConfig config,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator generator,
        StructureManager manager,
        BlockPos pos,
        StructurePiecesHolder children,
        Random rand,
        boolean b,
        boolean generateAtSurface,
        HeightLimitView view,
        CallbackInfo info
    ) {
        if(config instanceof ExtendedStructurePoolFeatureConfig) {
            featureConfig.set((ExtendedStructurePoolFeatureConfig)config);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @ModifyVariable(
        method = "generate",
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private static StructurePoolBasedGenerator.StructurePoolGenerator setElementMinMaxForStructure(
        StructurePoolBasedGenerator.StructurePoolGenerator gen
    ) {
        ExtendedStructurePoolFeatureConfig config = featureConfig.get();
        if(config != null) {
            Map<Identifier, ElementRange> ranges = new HashMap<>();
            for(ElementRange range : config.getRangeConstraints()) {
                ranges.put(range.id(), range);
            }
            StructurePoolGeneratorAddition gen1 = (StructurePoolGeneratorAddition)(Object)gen;
            gen1.structhelp_setRoomMinMax(ranges);
            featureConfig.remove();
            poolGenerator.set(gen1);
        }
        return gen;
    }

    /**
     * Generate child elements. Child element generation does not necessarily
     * respect total structure piece count nor placement limits.
     */
    @Inject(
        method = "generate",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
            remap = false
        )
    )
    private static void generateChildren(
        DynamicRegistryManager registryManager,
        StructurePoolFeatureConfig config,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator generator,
        StructureManager manager,
        BlockPos pos,
        StructurePiecesHolder holder,
        Random rand,
        boolean b,
        boolean generateAtSurface,
        HeightLimitView view,
        CallbackInfo info
    ) {
        StructurePoolGeneratorAddition gen = poolGenerator.get();
        if(gen != null) {
            poolGenerator.remove();
            gen.structhelp_setGeneratingChildren();
            // use a simple for-statement to fix the elements we are iterating over even while generatePiece()
            // adds child pieces to the list, while avoiding cloning the entire list. Hacky, but workable.
            var children = ((StructurePoolGeneratorAccessor)gen).getChildren();
            var numBasePieces = children.size();
            // noinspection ForLoopReplaceableByForEach
            for(var i = 0; i < numBasePieces; i++) {
                if(children.get(i) instanceof PoolStructurePiece poolPiece) {
                    BlockBox blockBox = poolPiece.getBoundingBox();
                    int x = (blockBox.getMaxX() + blockBox.getMinX()) / 2;
                    int z = (blockBox.getMaxZ() + blockBox.getMinZ()) / 2;
                    int y = generator.getHeightOnGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, view);
                    ((StructurePoolGeneratorAccessor)gen).callGeneratePiece(poolPiece,
                        new MutableObject<>(VoxelShapes.empty()),
                        y + 80,
                        0,
                        b,
                        view);
                }
            }
            if(!gen.structhelp_softCheckMinMaxConstraints()) {
                LogManager.getLogger(StructurePoolBasedGenerator.class)
                    .info("StructHelp - failed to satisfy range constraints");
            }
        }
    }

    @Redirect(
        method = "generate",
        at = @At(
            value = "NEW",
            target = "(DDDDDD)Lnet/minecraft/util/math/Box;"
        )
    )
    private static Box expandMaxStructureBounds(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        DynamicRegistryManager registryManager,
        StructurePoolFeatureConfig config,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator generator,
        StructureManager manager,
        BlockPos pos,
        StructurePiecesHolder children,
        Random rand,
        boolean b,
        boolean generateAtSurface,
        HeightLimitView view
    ) {
        final int vanillaExtent = 80;
        if(config instanceof ExtendedStructurePoolFeatureConfig data) {
            int extentH = data.getHorizontalExtent() - vanillaExtent;
            int extentV = data.getVerticalExtent() - vanillaExtent;
            if(extentH <= -vanillaExtent) {
                extentH = 0;
            }
            if(extentV <= -vanillaExtent) {
                extentV = 0;
            }
            return new Box(minX - extentH,
                minY - extentV,
                minZ - extentH,
                maxX + extentH,
                maxY + extentV,
                maxZ + extentH);
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
