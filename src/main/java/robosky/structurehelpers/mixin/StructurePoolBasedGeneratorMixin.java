package robosky.structurehelpers.mixin;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.Blocks;
import net.minecraft.block.JigsawBlock;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import robosky.structurehelpers.structure.pool.ExtendedSinglePoolElement;

/**
 * Modifies the structure pool element placement algorithm
 * to use the data provided by {@link ExtendedSinglePoolElement}
 * and co.
 */
@Mixin(targets = "net.minecraft.structure.pool.StructurePoolBasedGenerator$StructurePoolGenerator")
public abstract class StructurePoolBasedGeneratorMixin {

    @Unique
    private ExtendedSinglePoolElement elementToPlace;

    @Unique
    private PoolStructurePiece baseStructurePiece;

    // jigsaw offsets

    @Unique
    private int srcOffsetX;

    @Unique
    private int srcOffsetY;

    @Unique
    private int srcOffsetZ;

    @Unique
    private int dstOffsetX;

    @Unique
    private int dstOffsetY;

    @Unique
    private int dstOffsetZ;

    /**
     * Save method context that is needed but not available via
     * ModifyVariable.
     */
    @Inject(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        at = @At("HEAD")
    )
    private void saveLocalHeadState(PoolStructurePiece piece, AtomicReference<VoxelShape> atomicReference, int i, int j, CallbackInfo info) {
        baseStructurePiece = piece;
    }

    /**
     * Save the element that we are trying to place in order to avoid
     * capturing lots of local variables with an Inject.
     */
    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        ordinal = 1,
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private StructurePoolElement saveElementToPlace(StructurePoolElement element) {
        if(element instanceof ExtendedSinglePoolElement) {
            elementToPlace = (ExtendedSinglePoolElement)element;
        } else {
            elementToPlace = null;
        }
        return element;
    }

    /**
     * Modify the list of BlockRotations depending on the type requested
     * by the {@link ExtendedSinglePoolElement}.
     */
    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        ordinal = 2,
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private Iterator<BlockRotation> getRotations(Iterator<BlockRotation> itr) {
        if(elementToPlace != null) {
            assert baseStructurePiece != null : "getRotations - baseStructurePiece";
            switch(elementToPlace.rotationType()) {
                case NONE:
                    return ImmutableList.of(BlockRotation.NONE).iterator();
                case INHERITED:
                    return ImmutableList.of(baseStructurePiece.getRotation()).iterator();
                case RANDOM:
                default:
                    return itr;
            }
        } else {
            return itr;
        }
    }

    /**
     * Save the offset stored in the source jigsaw connection.
     * The saved value is reversed here because the offset is ultimately
     * applied to the destination.
     */
    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        ordinal = 0,
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private StructureBlockInfo saveSrcOffset(StructureBlockInfo blockInfo) {
        assert blockInfo.state.getBlock() == Blocks.JIGSAW;
        Direction dir = blockInfo.state.get(JigsawBlock.FACING);
        int offset = blockInfo.tag.getByte("StructHelp_Offset");
        srcOffsetX = srcOffsetY = srcOffsetZ = 0;
        switch(dir) {
            case DOWN:
                srcOffsetY += offset;
                break;
            case UP:
                srcOffsetY -= offset;
                break;
            case NORTH:
                srcOffsetZ += offset;
                break;
            case SOUTH:
                srcOffsetZ -= offset;
                break;
            case WEST:
                srcOffsetX += offset;
                break;
            case EAST:
                srcOffsetX -= offset;
                break;
        }
        return blockInfo;
    }

    /**
     * Save the offset stored in the destination jigsaw connection.
     */
    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        ordinal = 1,
        at = @At(
            value = "LOAD",
            ordinal = 1
        )
    )
    private StructureBlockInfo saveDestOffset(StructureBlockInfo blockInfo) {
        assert blockInfo.state.getBlock() == Blocks.JIGSAW;
        Direction dir = blockInfo.state.get(JigsawBlock.FACING);
        int offset = blockInfo.tag.getByte("StructHelp_Offset");
        dstOffsetX = dstOffsetY = dstOffsetZ = 0;
        switch(dir) {
            case DOWN:
                dstOffsetY -= offset;
                break;
            case UP:
                dstOffsetY += offset;
                break;
            case NORTH:
                dstOffsetZ -= offset;
                break;
            case SOUTH:
                dstOffsetZ += offset;
                break;
            case WEST:
                dstOffsetX -= offset;
                break;
            case EAST:
                dstOffsetX += offset;
                break;
        }
        return blockInfo;
    }

    /**
     * Translates the new piece according to the offsets defined in
     * the jigsaw block connection.
     */
    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        ordinal = 1,
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private PoolStructurePiece offsetPiece(PoolStructurePiece newPiece) {
        newPiece.translate(srcOffsetX + dstOffsetX, srcOffsetY + dstOffsetY, srcOffsetZ + dstOffsetZ);
        return newPiece;
    }

    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        ordinal = 3,
        at = @At(
            value = "LOAD",
            ordinal = 5
        )
    )
    private BlockBox offsetBox(BlockBox box) {
        return box.translated(srcOffsetX + dstOffsetX, srcOffsetY + dstOffsetY, srcOffsetZ + dstOffsetZ);
    }
}
