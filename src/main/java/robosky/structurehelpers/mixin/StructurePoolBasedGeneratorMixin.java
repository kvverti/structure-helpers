package robosky.structurehelpers.mixin;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.JigsawBlock;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.pool.EmptyPoolElement;
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

import org.apache.logging.log4j.LogManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import robosky.structurehelpers.iface.ElementRange;
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
    private Iterator<StructurePoolElement> elementIterator;

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

    /**
     * A map to keep track of how many times each structure pool element
     * is used in a single generation. Generation will continue until all
     * structure pool elements are used at least their minimum number of
     * times.
     */
    @Unique
    private final Object2IntMap<Identifier> elementUses = new Object2IntArrayMap<>();

    @Unique
    private final Map<Identifier, ElementRange> elementMinMax = new HashMap<>();

    @Shadow @Final private int maxSize;
    @Shadow @Final private List<StructurePiece> children;

    @Shadow
    private native void generatePiece(PoolStructurePiece piece, AtomicReference<VoxelShape> shape, int i, int j);

    /**
     * This is the most massive hack! Structure pool placement ranges
     * should be defined per structure, not per pool element, because
     * pool elements can be members of multiple pools. However, all the
     * generation happens in the constructor of this class, so any added
     * field with a setter would be initialized too late. Therefore, the
     * placement range information must be passed as a parameter. The least
     * hacky parameter with which to pass this info is the children out
     * parameter. This is a ModifyVariable injector because callback injectors
     * can only be injected at RETURN in constructors, which is too late.
     */
    @ModifyVariable(
        method = "<init>(Lnet/minecraft/util/Identifier;ILnet/minecraft/structure/pool/StructurePoolBasedGenerator$PieceFactory;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/structure/StructureManager;Lnet/minecraft/util/math/BlockPos;Ljava/util/List;Ljava/util/Random;)V",
        ordinal = 0,
        at = @At(
            value = "LOAD",
            ordinal = 0
        )
    )
    private List<?> extractRoomMinMax(List<?> ls) {
        if(!ls.isEmpty() && ls.get(0) instanceof ElementRange) {
            for(Object obj : ls) {
                ElementRange data = (ElementRange)obj;
                elementMinMax.put(data.id, data);
                elementUses.put(data.id, 0);
            }
            ls.clear();
        }
        return ls;
    }

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
     * Save the element iterator for use within the iteration to skip extended
     * elements that should not be placed.
     */
    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        ordinal = 1,
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private Iterator<StructurePoolElement> saveElementIterator(Iterator<StructurePoolElement> itr) {
        elementIterator = itr;
        return itr;
    }

    /**
     * Skip extended pool elements that have been placed their maximum number
     * of times, and save the selected element if it is extended.
     * As well, save the element that we are trying to place in order to avoid
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
        boolean elementValid;
        do {
            elementToPlace = null;
            elementValid = true;
            if(element instanceof ExtendedSinglePoolElement) {
                elementToPlace = (ExtendedSinglePoolElement)element;
                if(elementMinMax.containsKey(elementToPlace.location())) {
                    int uses = elementUses.getInt(elementToPlace.location());
                    if(uses + 1 > elementMinMax.get(elementToPlace.location()).max) {
                        if(elementIterator.hasNext()) {
                            element = elementIterator.next();
                        } else {
                            // fortunately, the code immediately after this
                            // checks for this value and breaks the loop if
                            // this value is found
                            element = EmptyPoolElement.INSTANCE;
                        }
                        elementValid = false;
                    }
                }
            }
        } while(!elementValid);
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
     * Increment the current extended pool element placement count.
     */
    @ModifyArg(
        method = {
            "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
            "<init>(Lnet/minecraft/util/Identifier;ILnet/minecraft/structure/pool/StructurePoolBasedGenerator$PieceFactory;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/structure/StructureManager;Lnet/minecraft/util/math/BlockPos;Ljava/util/List;Ljava/util/Random;)V"
        },
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            remap = false
        ),
        expect = 2
    )
    private Object incrementElementCount(Object obj) {
        // we can use this.elementToPlace because it is overwritten
        // on every iteration
        PoolStructurePiece piece = (PoolStructurePiece)obj;
        if(piece.getPoolElement() instanceof ExtendedSinglePoolElement) {
            ExtendedSinglePoolElement element = (ExtendedSinglePoolElement)piece.getPoolElement();
            System.out.println(element);
            if(elementMinMax.containsKey(element.location())) {
                elementUses.put(element.location(), elementUses.getInt(element.location()) + 1);
            }
        }
        return obj;
    }

    /**
     * Skips child junctions when generating normal connections and visa-versa.
     */
    @Redirect(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/JigsawBlock;attachmentMatches(Lnet/minecraft/structure/Structure$StructureBlockInfo;Lnet/minecraft/structure/Structure$StructureBlockInfo;)Z",
            ordinal = 0
        )
    )
    private boolean filterAppropriateJunctionType(StructureBlockInfo a, StructureBlockInfo b) {
        boolean child = a.tag.getBoolean(JigsawAccessorData.CHILD_JUNCTION);
        boolean connectingChild = b.tag.getBoolean(JigsawAccessorData.CHILD_JUNCTION);
        if(!connectingChild && (generatingChildren == child)) {
            return JigsawBlock.attachmentMatches(a, b);
        }
        return false;
    }

    /**
     * Disable the structure self-intersection check for generating
     * child elements.
     */
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

    /**
     * Prevent placing child elements in the queue for further processing.
     * Child elements are never operated upon recursively.
     * Also keeps (normal) generation going when not all extended pool
     * elements have been placed their minimum number of times.
     */
    @Redirect(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/concurrent/atomic/AtomicReference;II)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/structure/pool/StructurePoolBasedGenerator$StructurePoolGenerator;maxSize:I"
        ),
        expect = 2
    )
    private int preventRecursiveChildGen(@Coerce Object self, PoolStructurePiece piece, AtomicReference<VoxelShape> atomicReference, int i, int j) {
        if(generatingChildren) {
            return Integer.MIN_VALUE;
        } else {
            // if there are more than this number of generation steps
            // beyond the maximum size, assume unsatisfiable constrants
            // and force generation to stop
            final int MAX_EXTRA_ITRS = 4;
            if(j - this.maxSize <= MAX_EXTRA_ITRS) {
                for(Object2IntMap.Entry<Identifier> entry : elementUses.object2IntEntrySet()) {
                    if(entry.getIntValue() < elementMinMax.get(entry.getKey()).min) {
                        return Integer.MAX_VALUE;
                    }
                }
            }
            return this.maxSize;
        }
    }

    /**
     * Generate child elements. Child element generation does not necessarily
     * respect total structure piece count nor placement limits.
     */
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
        for(Object2IntMap.Entry<Identifier> entry : elementUses.object2IntEntrySet()) {
            if(entry.getIntValue() < elementMinMax.get(entry.getKey()).min) {
                LogManager.getLogger(getClass()).info("StructHelp - failed to satisfy range constraints");
                break;
            }
        }
    }
}
