package robosky.structurehelpers.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.arguments.BlockStateArgumentType;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableIntBoundingBox;
import net.minecraft.world.IWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import robosky.structurehelpers.StructureHelpers;

@Mixin(SimpleStructurePiece.class)
public abstract class SimpleStructurePieceMixin extends StructurePiece {

    @Unique
    private static final BlockStateArgumentType blockStateParser = BlockStateArgumentType.blockState();

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
                target = "Lnet/minecraft/structure/Structure;method_15172(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;I)Z"
            )
        )
    )
    private void handleLootData(IWorld world, Random rand, MutableIntBoundingBox box, ChunkPos chunkPos, CallbackInfoReturnable<Boolean> info) {
        List<Structure.StructureBlockInfo> ls = this.structure.method_16445(this.pos, this.placementData, StructureHelpers.LOOT_DATA_BLOCK);
        for (Structure.StructureBlockInfo bi : ls) {
            if (bi.tag != null) {
                BlockEntity be = world.getBlockEntity(bi.pos.down());
                if (be instanceof LootableContainerBlockEntity) {
                    LootableContainerBlockEntity lc = (LootableContainerBlockEntity)be;
                    Identifier lootTable;
                    BlockState blockState;
                    // loot table is validated by the block entity, the screen,
                    // and the packets both ways. If the loot table isn't a valid
                    // Identifier by this point, God help us.
                    try {
                        lootTable = new Identifier(bi.tag.getString("LootTable"));
                    } catch (InvalidIdentifierException e) {
                        lootTable = new Identifier("minecraft:empty");
                    }
                    try {
                        String replacement = bi.tag.getString("Replacement");
                        blockState = blockStateParser.method_9654(new StringReader(replacement)).getBlockState();
                    } catch (CommandSyntaxException e) {
                        blockState = Blocks.AIR.getDefaultState();
                    }
                    lc.setLootTable(lootTable, rand.nextLong());
                    world.setBlockState(bi.pos, blockState, 0);
                }
            }
        }
    }
}
