package robosky.structurehelpers.structure;

import java.util.Optional;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import robosky.structurehelpers.block.StructureRepeaterBlock;
import robosky.structurehelpers.block.StructureRepeaterBlockEntity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

/**
 * Contains misc. loot data handling functions.
 */
public final class ExtendedStructureHandling {

    private static final BlockStateArgumentType blockStateParser = BlockStateArgumentType.blockState();

    private ExtendedStructureHandling() {
    }

    public static void handleLootData(WorldAccess world, StructureBlockInfo bi) {
        if(bi.tag != null) {
            BlockEntity be = world.getBlockEntity(bi.pos.down());
            if(be instanceof LootableContainerBlockEntity) {
                LootableContainerBlockEntity lc = (LootableContainerBlockEntity)be;
                Identifier lootTable;
                BlockState blockState;
                // loot table is validated by the block entity, the screen,
                // and the packets both ways. If the loot table isn't a valid
                // Identifier by this point, God help us.
                try {
                    lootTable = new Identifier(bi.tag.getString("LootTable"));
                } catch(InvalidIdentifierException e) {
                    lootTable = new Identifier("minecraft:empty");
                }
                try {
                    String replacement = bi.tag.getString("Replacement");
                    blockState = blockStateParser.parse(new StringReader(replacement)).getBlockState();
                } catch(CommandSyntaxException e) {
                    blockState = Blocks.AIR.getDefaultState();
                }
                lc.setLootTable(lootTable, world.getRandom().nextLong());
                world.setBlockState(bi.pos, blockState, 0);
            }
        }
    }

    public static void handleRepeater(WorldAccess world, StructureBlockInfo bi) {
        if(bi.tag != null) {
            Direction dir = bi.state.get(StructureRepeaterBlock.FACING);
            int minRepeat = Math.max(0, bi.tag.getInt("RepeatMin"));
            int maxRepeat = Math.max(minRepeat, bi.tag.getInt("RepeatMax"));
            NbtCompound dataTag = bi.tag.getCompound("Data");
            // the data should be valid, barring incompatible updates to the NBT schema
            Optional<StructureRepeaterBlockEntity.RepeaterData> optionalData = StructureRepeaterBlockEntity.RepeaterData.CODEC
                .parse(NbtOps.INSTANCE, dataTag)
                .result();
            if(optionalData.isPresent()) {
                StructureRepeaterBlockEntity.RepeaterData data = optionalData.get();
                int repetitions = world.getRandom().nextInt(maxRepeat - minRepeat + 1) + minRepeat;
                switch(data.mode) {
                    // repeat a single block state
                    case SINGLE: {
                        BlockState state;
                        try {
                            state = blockStateParser.parse(new StringReader(data.asSingle().serializedState)).getBlockState();
                        } catch(CommandSyntaxException e) {
                            state = Blocks.AIR.getDefaultState();
                        }
                        BlockPos.Mutable pos = new BlockPos.Mutable();
                        pos.set(bi.pos);
                        for(int i = 0; i < repetitions; i++) {
                            pos.move(dir);
                            world.setBlockState(pos, state, 0);
                        }
                        break;
                    }
                    case LAYER:
                        break;
                    case JIGSAW:
                        // this was already taken care of
                        break;
                }
            }
        }
    }
}
