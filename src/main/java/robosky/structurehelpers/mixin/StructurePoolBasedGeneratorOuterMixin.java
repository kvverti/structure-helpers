package robosky.structurehelpers.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import robosky.structurehelpers.iface.ExtendedStructurePoolBasedGeneratorData;
import robosky.structurehelpers.iface.StructurePoolGeneratorAddition;
import robosky.structurehelpers.structure.pool.ElementRange;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Note: because this class is accessed from multiple threads, there is no mutable
 * static state added to this class. Instead, the state is passed into this class
 * via the {@code List} method parameter.
 *
 * @see ExtendedStructurePoolBasedGeneratorData
 */
@Mixin(StructurePoolBasedGenerator.class)
public abstract class StructurePoolBasedGeneratorOuterMixin {

    /**
     * Extract element placement ranges from the child element list
     * out parameter.
     */
    @Inject(method = "addPieces", at = @At("HEAD"))
    private static void extractRoomMinMax(
        Identifier startPoolId,
        int size,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator<?> chunkGenerator,
        StructureManager structureManager,
        BlockPos blockPos,
        List<?> ls,
        Random random,
        boolean bl,
        boolean bl2,
        CallbackInfo info
    ) {
        if(ls instanceof ExtendedStructurePoolBasedGeneratorData) {
            ExtendedStructurePoolBasedGeneratorData data = (ExtendedStructurePoolBasedGeneratorData)ls;
            for(ElementRange range : data.getElementPlacementRanges()) {
                data.putElementMinMax(range.id, range);
            }
            ls.clear();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @ModifyVariable(
        method = "addPieces",
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private static StructurePoolBasedGenerator.StructurePoolGenerator setElementMinMaxForStructure(
        StructurePoolBasedGenerator.StructurePoolGenerator gen
    ) {
        // we get our parameters through gen.children
        List<? super PoolStructurePiece> pieces = ((StructurePoolGeneratorAccessor)(Object)gen).getChildren();
        if(pieces instanceof ExtendedStructurePoolBasedGeneratorData) {
            ((ExtendedStructurePoolBasedGeneratorData)pieces).setPoolGenerator((StructurePoolGeneratorAddition)(Object)gen);
        }
        return gen;
    }

    /**
     * Generate child elements. Child element generation does not necessarily
     * respect total structure piece count nor placement limits.
     */
    @Inject(method = "addPieces", at = @At("RETURN"))
    private static void generateChildren(
        Identifier id,
        int i,
        StructurePoolBasedGenerator.PieceFactory factory,
        ChunkGenerator<?> generator,
        StructureManager manager,
        BlockPos pos,
        List<PoolStructurePiece> pieces,
        Random rand,
        boolean b1,
        boolean b2,
        CallbackInfo info
    ) {
        if(pieces instanceof ExtendedStructurePoolBasedGeneratorData) {
            ExtendedStructurePoolBasedGeneratorData data = (ExtendedStructurePoolBasedGeneratorData)pieces;
            data.getPoolGenerator().structhelp_setGeneratingChildren();
            for(StructurePiece piece : new ArrayList<>(pieces)) {
                if(piece instanceof PoolStructurePiece) {
                    PoolStructurePiece poolPiece = (PoolStructurePiece)piece;
                    BlockBox blockBox = poolPiece.getBoundingBox();
                    int x = (blockBox.maxX + blockBox.minX) / 2;
                    int z = (blockBox.maxZ + blockBox.minZ) / 2;
                    int y = generator.getHeightOnGround(x, z, Heightmap.Type.WORLD_SURFACE_WG);
                    ((StructurePoolGeneratorAccessor)data.getPoolGenerator()).callGeneratePiece(poolPiece,
                        new AtomicReference<>(VoxelShapes.empty()),
                        y + 80,
                        0,
                        b1);
                }
            }
            if(!data.getPoolGenerator().structhelp_softCheckMinMaxConstraints()) {
                LogManager.getLogger(StructurePoolBasedGenerator.class)
                    .info("StructHelp - failed to satisfy range constraints");
            }
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(
        method = "addPieces",
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
        Identifier id,
        int i,
        StructurePoolBasedGenerator.PieceFactory factory,
        ChunkGenerator<?> generator,
        StructureManager manager,
        BlockPos pos,
        List<PoolStructurePiece> pieces,
        Random rand,
        boolean b1,
        boolean b2
    ) {
        final int vanillaExtent = 80;
        if(pieces instanceof ExtendedStructurePoolBasedGeneratorData) {
            ExtendedStructurePoolBasedGeneratorData data = (ExtendedStructurePoolBasedGeneratorData)pieces;
            int extentH = data.getExtentH() - vanillaExtent;
            int extentV = data.getExtentV() - vanillaExtent;
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
