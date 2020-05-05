package robosky.structurehelpers.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import robosky.structurehelpers.iface.ElementRange;
import robosky.structurehelpers.iface.StructurePoolGeneratorAccessor;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;

@Mixin(StructurePoolBasedGenerator.class)
public abstract class StructurePoolBasedGeneratorOuterMixin {

    @Unique
    private static final Map<Identifier, ElementRange> elementMinMax = new HashMap<>();

    @Unique
    private static StructurePoolBasedGenerator.StructurePoolGenerator poolGenerator;

    /**
     * Extract element placement ranges from the child element list
     * out parameter.
     */
    @Inject(method = "addPieces", at = @At("HEAD"))
    private static void extractRoomMinMax(
        Identifier startPoolId,
        int size,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator<?> chunkGenerator,
        StructureManager structureManager,
        BlockPos blockPos,
        List<?> ls,
        Random random,
        boolean bl,
        boolean bl2,
        CallbackInfo info
    ) {
        if(!ls.isEmpty() && ls.get(0) instanceof ElementRange) {
            for(Object obj : ls) {
                ElementRange data = (ElementRange)obj;
                elementMinMax.put(data.id, data);
            }
            ls.clear();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @ModifyVariable(
        method = "addPieces",
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private static StructurePoolBasedGenerator.StructurePoolGenerator setElementMinMaxForStructure(
        StructurePoolBasedGenerator.StructurePoolGenerator gen
    ) {
        ((StructurePoolGeneratorAccessor)(Object)gen).structhelp_setRoomMinMax(elementMinMax);
        poolGenerator = gen;
        return gen;
    }

    /**
     * Generate child elements. Child element generation does not necessarily
     * respect total structure piece count nor placement limits.
     */
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "addPieces", at = @At("RETURN"))
    private static void generateChildren(
        Identifier id,
        int i,
        StructurePoolBasedGenerator.PieceFactory factory,
        ChunkGenerator<?> generator,
        StructureManager manager,
        BlockPos pos,
        List<StructurePiece> pieces,
        Random rand,
        boolean b1,
        boolean b2,
        CallbackInfo info
    ) {
        ((StructurePoolGeneratorAccessor)(Object)poolGenerator).structhelp_setGeneratingChildren();
        for(StructurePiece piece : new ArrayList<>(pieces)) {
            if(piece instanceof PoolStructurePiece) {
                PoolStructurePiece poolPiece = (PoolStructurePiece)piece;
                BlockBox blockBox = poolPiece.getBoundingBox();
                int x = (blockBox.maxX + blockBox.minX) / 2;
                int z = (blockBox.maxZ + blockBox.minZ) / 2;
                int y = generator.getHeightOnGround(x, z, Heightmap.Type.WORLD_SURFACE_WG);
                poolGenerator.generatePiece(poolPiece, new AtomicReference<>(VoxelShapes.empty()), y + 80, 0, b1);
            }
        }
        if(!((StructurePoolGeneratorAccessor)(Object)poolGenerator).structhelp_softCheckMinMaxConstraints()) {
            LogManager.getLogger(StructurePoolBasedGenerator.class)
                .info("StructHelp - failed to satisfy range constraints");
        }
    }
}
