package robosky.structurehelpers.mixin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import robosky.structurehelpers.iface.JigsawAccessorData;
import robosky.structurehelpers.iface.StructurePoolGeneratorAddition;
import robosky.structurehelpers.structure.pool.ElementRange;
import robosky.structurehelpers.structure.pool.ExtendedSinglePoolElement;

import net.minecraft.block.JigsawBlock;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.pool.EmptyPoolElement;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * Modifies the structure pool element placement algorithm
 * to use the data provided by {@link ExtendedSinglePoolElement}
 * and co.
 */
@Mixin(StructurePoolBasedGenerator.StructurePoolGenerator.class)
abstract class StructurePoolBasedGeneratorMixin implements StructurePoolGeneratorAddition {

    @Unique
    private Iterator<StructurePoolElement> elementIterator;

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

    @Shadow
    @Final
    private int maxSize;

    @Override
    public void structhelp_setRoomMinMax(Map<Identifier, ElementRange> elementMinMax) {
        this.elementMinMax.putAll(elementMinMax);
        for(Identifier id : elementMinMax.keySet()) {
            elementUses.put(id, 0);
        }
    }

    @Override
    public void structhelp_setGeneratingChildren() {
        generatingChildren = true;
    }

    @Override
    public boolean structhelp_softCheckMinMaxConstraints() {
        for(Object2IntMap.Entry<Identifier> entry : elementUses.object2IntEntrySet()) {
            if(entry.getIntValue() < elementMinMax.get(entry.getKey()).min) {
                return false;
            }
        }
        return true;
    }

    /**
     * Save the element iterator for use within the iteration to skip extended
     * elements that should not be placed.
     */
    @ModifyVariable(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IIZLnet/minecraft/world/HeightLimitView;)V",
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
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IIZLnet/minecraft/world/HeightLimitView;)V",
        ordinal = 1,
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private StructurePoolElement saveElementToPlace(StructurePoolElement element) {
        boolean elementValid;
        do {
            ExtendedSinglePoolElement tmpElement;
            elementValid = true;
            if(element instanceof ExtendedSinglePoolElement) {
                tmpElement = (ExtendedSinglePoolElement)element;
                if(elementMinMax.containsKey(tmpElement.location())) {
                    int uses = elementUses.getInt(tmpElement.location());
                    if(uses + 1 > elementMinMax.get(tmpElement.location()).max) {
                        if(elementIterator.hasNext()) {
                            element = elementIterator.next();
                            elementValid = false;
                        } else {
                            // fortunately, the code immediately after this
                            // checks for this value and breaks the loop if
                            // this value is found
                            element = EmptyPoolElement.INSTANCE;
                        }
                    }
                }
            }
        } while(!elementValid);
        return element;
    }

    /**
     * Increment the current extended pool element placement count.
     */
    @ModifyArg(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IIZLnet/minecraft/world/HeightLimitView;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            remap = false
        )
    )
    private Object incrementElementCount(Object obj) {
        // we can use this.elementToPlace because it is overwritten
        // on every iteration
        PoolStructurePiece piece = (PoolStructurePiece)obj;
        if(piece.getPoolElement() instanceof ExtendedSinglePoolElement) {
            ExtendedSinglePoolElement element = (ExtendedSinglePoolElement)piece.getPoolElement();
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
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IIZLnet/minecraft/world/HeightLimitView;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/JigsawBlock;attachmentMatches(Lnet/minecraft/structure/Structure$StructureBlockInfo;Lnet/minecraft/structure/Structure$StructureBlockInfo;)Z",
            ordinal = 0
        )
    )
    private boolean filterAppropriateJunctionType(StructureBlockInfo a, StructureBlockInfo b) {
        boolean child = a.nbt.getBoolean(JigsawAccessorData.CHILD_JUNCTION);
        boolean connectingChild = b.nbt.getBoolean(JigsawAccessorData.CHILD_JUNCTION);
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
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IIZLnet/minecraft/world/HeightLimitView;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/shape/VoxelShapes;matchesAnywhere(Lnet/minecraft/util/shape/VoxelShape;Lnet/minecraft/util/shape/VoxelShape;Lnet/minecraft/util/function/BooleanBiFunction;)Z",
            ordinal = 0
        )
    )
    private boolean disableBoundsCheckForChildGen(VoxelShape a, VoxelShape b, BooleanBiFunction filter) {
        return !generatingChildren && VoxelShapes.matchesAnywhere(a, b, filter);
    }

    /**
     * Prevent placing child elements in the queue for further processing.
     * Child elements are never operated upon recursively.
     * Also keeps (normal) generation going when not all extended pool
     * elements have been placed their minimum number of times.
     */
    @Redirect(
        method = "generatePiece(Lnet/minecraft/structure/PoolStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IIZLnet/minecraft/world/HeightLimitView;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/structure/pool/StructurePoolBasedGenerator$StructurePoolGenerator;maxSize:I"
        ),
        expect = 2
    )
    private int preventRecursiveChildGen(
        StructurePoolBasedGenerator.StructurePoolGenerator self,
        PoolStructurePiece piece,
        MutableObject<VoxelShape> atomicReference,
        int minY,
        int currentSize,
        boolean b1
    ) {
        // note: self == this
        if(generatingChildren) {
            // allow placing non-terminal children, but not recursive generation
            // (currentSize != maxSize) is true, (currentSize + 1 <= maxSize) is false
            return Integer.MIN_VALUE;
        } else {
            // if there are more than this number of generation steps
            // beyond the maximum size, assume unsatisfiable constraints
            // and force generation to stop
            final int MAX_EXTRA_ITRS = 4;
            if(currentSize - this.maxSize <= MAX_EXTRA_ITRS) {
                for(Object2IntMap.Entry<Identifier> entry : elementUses.object2IntEntrySet()) {
                    if(entry.getIntValue() < elementMinMax.get(entry.getKey()).min) {
                        // continue normal generation past max size
                        // (currentSize != maxSize) is true, (currentSize + 1 <= maxSize) is true
                        return Integer.MAX_VALUE;
                    }
                }
            }
            // preserve vanilla behavior, extending terminal-only placement to
            // iterations past the max
            // If currentSize >= maxSize:
            //   (currentSize != maxSize) is false, (currentSize + 1 <= maxSize) is false
            // If currentSize < maxSize:
            //   (currentSize != maxSize) is true, (currentSize + 1 <= maxSize) is true
            return Math.max(this.maxSize, currentSize);
        }
    }
}
