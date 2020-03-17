package robosky.structurehelpers.mixin;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.JigsawBlock;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.pool.StructurePoolBasedGenerator.PieceFactory;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import robosky.structurehelpers.iface.JigsawAccessorData;
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
     * When structure piece children are generated, the code that checks
     * for structure self-intersection is disabled.
     */
    @Unique
    private boolean generatingChildren;

    @Shadow @Final private int maxSize;
    @Shadow @Final private List<StructurePiece> children;

    @Shadow
    private native void generatePiece(PoolStructurePiece piece, AtomicReference<VoxelShape> shape, int i, int j);

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

    @Redirect(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/JigsawBlock;attachmentMatches(Lnet/minecraft/structure/Structure$StructureBlockInfo;Lnet/minecraft/structure/Structure$StructureBlockInfo;)Z",
            ordinal = 0
        )
    )
    private boolean skipAppropriateStructureBlocks(StructureBlockInfo a, StructureBlockInfo b) {
        boolean child = a.tag.getBoolean(JigsawAccessorData.CHILD_JUNCTION);
        boolean connectingChild = b.tag.getBoolean(JigsawAccessorData.CHILD_JUNCTION);
        if(!connectingChild && (generatingChildren == child)) {
            return JigsawBlock.attachmentMatches(a, b);
        }
        return false;
    }

    @Redirect(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/shape/VoxelShapes;matchesAnywhere(Lnet/minecraft/util/shape/VoxelShape;Lnet/minecraft/util/shape/VoxelShape;Lnet/minecraft/util/BooleanBiFunction;)Z",
            ordinal = 0
        )
    )
    private boolean disableBoundsCheckForChildGen(VoxelShape a, VoxelShape b, BooleanBiFunction filter) {
        return generatingChildren ? false : VoxelShapes.matchesAnywhere(a, b, filter);
    }

    @Redirect(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/structure/pool/StructurePoolBasedGenerator$StructurePoolGenerator;maxSize:I",
            ordinal = 1
        )
    )
    private int preventRecursiveChildGen(@Coerce Object self) {
        return generatingChildren ? 0 : this.maxSize;
    }

    @Inject(
        method = "<init>(Lnet/minecraft/util/Identifier;ILnet/minecraft/structure/pool/StructurePoolBasedGenerator$PieceFactory;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/structure/StructureManager;Lnet/minecraft/util/math/BlockPos;Ljava/util/List;Ljava/util/Random;)V",
        at = @At("RETURN")
    )
    private void generateChildren(Identifier id, int i, PieceFactory factory, ChunkGenerator<?> generator, StructureManager manager, BlockPos pos, List<StructurePiece> pieces, Random rand, CallbackInfo info) {
        generatingChildren = true;
        for(StructurePiece piece : new ArrayList<>(this.children)) {
            if(piece instanceof PoolStructurePiece) {
                PoolStructurePiece poolPiece = (PoolStructurePiece)piece;
                BlockBox blockBox = poolPiece.getBoundingBox();
                int x = (blockBox.maxX + blockBox.minX) / 2;
                int z = (blockBox.maxZ + blockBox.minZ) / 2;
                int y = generator.method_20402(x, z, Heightmap.Type.WORLD_SURFACE_WG);
                this.generatePiece(poolPiece, new AtomicReference<>(VoxelShapes.empty()), y + 80, 0);
            }
        }
    }
}
