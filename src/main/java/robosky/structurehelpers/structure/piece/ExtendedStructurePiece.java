package robosky.structurehelpers.structure.piece;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * A type-polymorphic structure piece meant to be used with extended structure features.
 */
public class ExtendedStructurePiece extends PoolStructurePiece {

    /**
     * Constructor template for {@link StructurePoolBasedGenerator.PieceFactory}.
     */
    protected ExtendedStructurePiece(
        StructurePieceType type,
        StructureManager manager,
        StructurePoolElement element,
        BlockPos pos,
        int groundDelta,
        BlockRotation rotation,
        BlockBox box
    ) {
        super(type, manager, element, pos, groundDelta, rotation, box);
    }

    /**
     * Constructor template for {@link StructurePieceType}.
     */
    protected ExtendedStructurePiece(StructurePieceType type, StructureManager mgr, CompoundTag tag) {
        super(mgr, tag, type);
    }

    /**
     * Creates a new {@link Factory} for an {@link ExtendedStructurePiece}. This factory may be used
     * as both a {@link StructurePieceType} and a {@link StructurePoolBasedGenerator.PieceFactory}.
     *
     * <p>The recommended way to use this method is as follows.
     * <code><pre>
     * public static final ExtendedStructurePiece.Factory PIECE_TYPE = Registry.register(
     *     Registry.STRUCTURE_PIECE,
     *     new Identifier("mod_id", "my_type"),
     *     ExtendedStructurePiece.newFactory()
     * );</pre>
     * </code>
     *
     * @see Factory
     */
    public static Factory newFactory() {
        return new Factory() {
            @Override
            public StructurePiece load(StructureManager manager, CompoundTag tag) {
                return new ExtendedStructurePiece(this, manager, tag);
            }

            @Override
            public PoolStructurePiece create(
                StructureManager manager,
                StructurePoolElement element,
                BlockPos pos,
                int groundDelta,
                BlockRotation rotation,
                BlockBox bounds
            ) {
                return new ExtendedStructurePiece(this, manager, element, pos, groundDelta, rotation, bounds);
            }
        };
    }

    /**
     * An interface that combines the interfaces of {@link StructurePieceType} and
     * {@link StructurePoolBasedGenerator.PieceFactory}.
     *
     * @see StructurePieceType
     * @see StructurePoolBasedGenerator.PieceFactory
     */
    public interface Factory extends StructurePieceType, StructurePoolBasedGenerator.PieceFactory { }
}
