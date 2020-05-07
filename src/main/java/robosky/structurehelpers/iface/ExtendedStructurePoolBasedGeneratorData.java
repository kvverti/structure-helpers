package robosky.structurehelpers.iface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ForwardingList;
import robosky.structurehelpers.structure.pool.ElementRange;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;

/**
 * Additional state for {@link StructurePoolBasedGenerator#addPieces}. This state is not stored statically
 * in {@link StructurePoolBasedGenerator} because the class is accessed off-thread, and there is a chance
 * that multiple worker threads may access the class in parallel. This extra data is passed to
 * {@link StructurePoolBasedGenerator#addPieces} by marshalling it through the {@code List<? super StructurePiece>}
 * parameter, hence why this class extends {@link ForwardingList}.
 *
 * @see StructurePoolBasedGenerator#addPieces
 * @see ForwardingList
 */
public final class ExtendedStructurePoolBasedGeneratorData extends ForwardingList<PoolStructurePiece> {

    /**
     * The underlying {@link List} that is returned to clients. We don't want to
     * keep around stale data, after all!
     */
    private final List<PoolStructurePiece> delegate;

    // additional parameters

    private final List<? extends ElementRange> elementPlacementRanges;
    private final int extentH;
    private final int extentV;

    // additional method state

    private final Map<Identifier, ElementRange> elementMinMax = new HashMap<>();
    private StructurePoolGeneratorAddition poolGenerator;

    public ExtendedStructurePoolBasedGeneratorData(
        List<? extends ElementRange> elementPlacementRanges,
        int extentH,
        int extentV
    ) {
        this.delegate = new ArrayList<>();
        this.elementPlacementRanges = elementPlacementRanges;
        this.extentH = extentH;
        this.extentV = extentV;
    }

    @Override
    public List<PoolStructurePiece> delegate() {
        return delegate;
    }

    public List<? extends ElementRange> getElementPlacementRanges() {
        return elementPlacementRanges;
    }

    public int getExtentH() {
        return extentH;
    }

    public int getExtentV() {
        return extentV;
    }

    public StructurePoolGeneratorAddition getPoolGenerator() {
        return poolGenerator;
    }

    public void setPoolGenerator(StructurePoolGeneratorAddition poolGenerator) {
        this.poolGenerator = poolGenerator;
        this.poolGenerator.structhelp_setRoomMinMax(elementMinMax);
    }

    public void putElementMinMax(Identifier id, ElementRange range) {
        elementMinMax.put(id, range);
    }
}
