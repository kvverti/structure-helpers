package robosky.structurehelpers.mixin;

import java.util.List;
import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.structure.ExtendedStructureHandling;

import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

@Mixin(SimpleStructurePiece.class)
abstract class SimpleStructurePieceMixin extends StructurePiece {

    @Shadow
    protected Structure structure;

    @Shadow
    protected StructurePlacementData placementData;

    @Shadow
    protected BlockPos pos;

    private SimpleStructurePieceMixin() {
        super(null, null);
    }

    // inject after the structure successfully generates
    @Inject(
        method = "generate",
        at = @At(value = "JUMP", ordinal = 0, shift = At.Shift.AFTER),
        slice = @Slice(
            from = @At(
                value = "INVOKE:FIRST",
                target = "Lnet/minecraft/structure/Structure;place(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;Ljava/util/Random;I)Z"
            )
        )
    )
    private void handleExtendedMetadata(
        StructureWorldAccess world,
        StructureAccessor accessor,
        ChunkGenerator generator,
        Random rand,
        BlockBox box,
        ChunkPos chunkPos,
        BlockPos blockPos,
        CallbackInfoReturnable<Boolean> info
    ) {
        List<Structure.StructureBlockInfo> lootData = this.structure.getInfosForBlock(this.pos,
            this.placementData,
            StructureHelpers.LOOT_DATA_BLOCK);
        for(Structure.StructureBlockInfo bi : lootData) {
            ExtendedStructureHandling.handleLootData(world, bi);
        }
        List<Structure.StructureBlockInfo> repeaters = this.structure.getInfosForBlock(this.pos,
            this.placementData,
            StructureHelpers.STRUCTURE_REPEATER_BLOCK);
        for(Structure.StructureBlockInfo bi : repeaters) {
            ExtendedStructureHandling.handleRepeater(world, bi);
        }
    }
}
