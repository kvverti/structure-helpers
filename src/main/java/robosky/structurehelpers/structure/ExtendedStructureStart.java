package robosky.structurehelpers.structure;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import robosky.structurehelpers.structure.pool.ElementRange;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;

/**
 * A structure start for generating extended structures.
 */
public class ExtendedStructureStart extends StructureStart {

    /**
     * The ID of the start pool this structure should use.
     */
    private final Identifier startPoolId;

    /**
     * The factory for structure pieces for this structure.
     */
    private final StructurePoolBasedGenerator.PieceFactory pieceFactory;

    /**
     * Minimum and maximum generation counts for given structure pool elements.
     */
    private final ImmutableList<ElementRange> permittedElementRanges;

    /**
     * Whether to find the world's surface and generate the start there.
     */
    private final boolean generateAtSurface;

    /**
     * Sole constructor, for use by anyone who needs a subclass.
     */
    protected ExtendedStructureStart(
        Identifier startPoolId,
        StructurePoolBasedGenerator.PieceFactory factory,
        boolean generateAtSurface,
        ImmutableList<ElementRange> permittedElementRanges,
        StructureFeature<?> feature,
        int chunkX,
        int chunkZ,
        BlockBox box,
        int refs,
        long seed
    ) {
        super(feature, chunkX, chunkZ, box, refs, seed);
        this.startPoolId = startPoolId;
        this.pieceFactory = factory;
        this.generateAtSurface = generateAtSurface;
        this.permittedElementRanges = permittedElementRanges;
    }

    /**
     * Creates a {@link StructureFeature.StructureStartFactory} with the given customization.
     *
     * @param startPoolId            The ID of the start pool for a structure.
     * @param pieceFactory           A factory for structure pieces.
     * @param generateAtSurface      If true, the start will always generate at the surface.
     * @param permittedElementRanges A list of count ranges which the generator must ensure placed
     *                               elements satisfy. These are soft ranges; the generator may opt
     *                               not to satisfy these constraints if it deems them unfeasible.
     * @return A StructureStartFactory.
     * @see #configure(Identifier, StructurePoolBasedGenerator.PieceFactory, boolean, ElementRange...)
     */
    public static StructureFeature.StructureStartFactory configure(
        Identifier startPoolId,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        boolean generateAtSurface,
        ImmutableList<ElementRange> permittedElementRanges
    ) {
        return (f, x, z, b, r, s) -> new ExtendedStructureStart(startPoolId,
            pieceFactory,
            generateAtSurface,
            permittedElementRanges,
            f,
            x,
            z,
            b,
            r,
            s);
    }

    /**
     * Creates a {@link StructureFeature.StructureStartFactory} with the given customization.
     *
     * @param startPoolId            The ID of the start pool for a structure.
     * @param pieceFactory           A factory for structure pieces.
     * @param generateAtSurface      If true, the start will always generate at the surface.
     * @param permittedElementRanges An array of count ranges which the generator must ensure placed
     *                               elements satisfy. These are soft ranges; the generator may opt
     *                               not to satisfy these constraints if it deems them unfeasible.
     * @return A StructureStartFactory.
     * @see #configure(Identifier, StructurePoolBasedGenerator.PieceFactory, boolean, ImmutableList)
     */
    public static StructureFeature.StructureStartFactory configure(
        Identifier startPoolId,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        boolean generateAtSurface,
        ElementRange... permittedElementRanges
    ) {
        return (f, x, z, b, r, s) -> new ExtendedStructureStart(startPoolId,
            pieceFactory,
            generateAtSurface,
            ImmutableList.copyOf(permittedElementRanges),
            f,
            x,
            z,
            b,
            r,
            s);
    }

    @Override
    public void init(ChunkGenerator<?> generator, StructureManager manager, int chunkX, int chunkZ, Biome biome) {
        List<ElementRange> ranges = new ArrayList<>(permittedElementRanges);
        // purposeful heap pollution - ranges go in, pieces come out >:)
        @SuppressWarnings("unchecked")
        List<PoolStructurePiece> pieces = (List<PoolStructurePiece>)(List<?>)ranges;
        StructurePoolBasedGenerator.addPieces(
            startPoolId,
            8,
            pieceFactory,
            generator,
            manager,
            new BlockPos(chunkX * 16, 0, chunkZ * 16),
            pieces,
            this.random,
            generateAtSurface, // don't know what this does yet
            generateAtSurface);
        this.children.addAll(pieces);
        this.setBoundingBoxFromChildren();
    }
}
