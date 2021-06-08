package robosky.structurehelpers.mixin;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.HeightLimitView;

/**
 * Accessors for {@link StructurePoolBasedGenerator.StructurePoolGenerator}.
 */
@Mixin(StructurePoolBasedGenerator.StructurePoolGenerator.class)
public interface StructurePoolGeneratorAccessor {

    @Accessor
    List<? super PoolStructurePiece> getChildren();

    @Invoker
    void callGeneratePiece(
        PoolStructurePiece piece,
        MutableObject<VoxelShape> pieceShape,
        int minY,
        int currentSize,
        boolean bl,
        HeightLimitView world
    );
}
