package robosky.structurehelpers.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import robosky.structurehelpers.structure.pool.ElementRange;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public final class ExtendedStructures {

    private ExtendedStructures() {
    }

    /**
     * Wrapper for {@link StructurePoolBasedGenerator#addPieces} that handles passing element placement
     * ranges.
     *
     * @param ranges A list of element placement ranges.
     * @return A list of placed structure elements.
     */
    public static List<PoolStructurePiece> addPieces(
        List<? extends ElementRange> ranges,
        Identifier startPoolId,
        int size,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator<?> generator,
        StructureManager manager,
        BlockPos pos,
        Random random,
        boolean b1,
        boolean generateAtSurface
    ) {
        // purposeful heap pollution - ranges go in, pieces come out >:)
        @SuppressWarnings("unchecked")
        List<PoolStructurePiece> children = (List<PoolStructurePiece>)(List<?>)new ArrayList<>(ranges);
        StructurePoolBasedGenerator.addPieces(
            startPoolId,
            size,
            pieceFactory,
            generator,
            manager,
            pos,
            children,
            random,
            b1, // don't know what this does yet
            generateAtSurface);
        return children;
    }
}
