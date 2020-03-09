package robosky.structurehelpers.mixin;

import net.minecraft.structure.pool.StructurePoolElement;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.util.BlockRotation;
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
}
