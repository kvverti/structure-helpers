package robosky.structurehelpers.structure;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.arguments.BlockStateArgumentType;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.world.IWorld;

/**
 * Contains misc. loot data handling functions.
 */
public final class LootDataUtil {

    private static final BlockStateArgumentType blockStateParser = BlockStateArgumentType.blockState();

    private LootDataUtil() {
    }

    public static void handleLootData(IWorld world, StructureBlockInfo bi) {
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
}
