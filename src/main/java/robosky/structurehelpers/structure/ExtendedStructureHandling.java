package robosky.structurehelpers.structure;

import java.util.Map;
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
import net.minecraft.state.State;
import net.minecraft.state.property.Property;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldAccess;

/**
 * Contains misc. loot data handling functions.
 */
public final class ExtendedStructureHandling {

    private static final BlockStateArgumentType blockStateParser = BlockStateArgumentType.blockState();

    private ExtendedStructureHandling() {
    }

    /**
     * Parse a command string into a block state, or return air if a block state cannot be parsed.
     */
    public static BlockState parseBlockState(String str) {
        try {
            return blockStateParser.parse(new StringReader(str)).getBlockState();
        } catch(CommandSyntaxException e) {
            return Blocks.AIR.getDefaultState();
        }
    }

    /**
     * Determines whether a block state string represents a valid block state.
     */
    public static boolean isValidBlockState(String str) {
        try {
            blockStateParser.parse(new StringReader(str));
            return true;
        } catch(CommandSyntaxException e) {
            return false;
        }
    }

    /**
     * Converts a block state to a string, removing defaulted properties from the output.
     * This method differs from {@link State#toString()} in that it removes properties
     * whose values are equal to the default.
     */
    public static String stringifyBlockState(BlockState state) {
        String blockId = Registry.BLOCK.getId(state.getBlock()).toString();
        BlockState defaultState = state.getBlock().getDefaultState();
        StringBuilder sb = new StringBuilder();
        // append non-defaulted property-value pairs
        sb.append('[');
        for(Map.Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet()) {
            Property<?> prop = entry.getKey();
            Comparable<?> value = entry.getValue();
            if(!propertyEquals(defaultState, prop, value)) {
                sb.append(prop.getName());
                sb.append('=');
                sb.append(valueName(prop, value));
                sb.append(',');
            }
        }
        sb.setCharAt(sb.length() - 1, ']');
        // leave off empty bracket pair, for aesthetic reasons
        if(sb.length() > 2) {
            return blockId + sb.toString();
        } else {
            return blockId;
        }
    }

    private static <T extends Comparable<T>> boolean propertyEquals(BlockState defaultState, Property<T> prop, Comparable<?> value) {
        return defaultState.get(prop).compareTo(prop.getType().cast(value)) == 0;
    }

    private static <T extends Comparable<T>> String valueName(Property<T> prop, Comparable<?> value) {
        return prop.name(prop.getType().cast(value));
    }

    public static void handleLootData(WorldAccess world, StructureBlockInfo bi) {
        if(bi.tag != null) {
            BlockEntity be = world.getBlockEntity(bi.pos.down());
            if(be instanceof LootableContainerBlockEntity) {
                LootableContainerBlockEntity lc = (LootableContainerBlockEntity)be;
                Identifier lootTable;
                // loot table is validated by the block entity, the screen,
                // and the packets both ways. If the loot table isn't a valid
                // Identifier by this point, God help us.
                try {
                    lootTable = new Identifier(bi.tag.getString("LootTable"));
                } catch(InvalidIdentifierException e) {
                    lootTable = new Identifier("minecraft:empty");
                }
                BlockState replacement = parseBlockState(bi.tag.getString("Replacement"));
                lc.setLootTable(lootTable, world.getRandom().nextLong());
                world.setBlockState(bi.pos, replacement, 0);
            }
        }
    }

    public static void handleRepeater(WorldAccess world, StructureBlockInfo bi) {
        if(bi.tag != null) {
            Direction dir = bi.state.get(StructureRepeaterBlock.FACING);
            int minRepeat = Math.max(0, bi.tag.getInt("RepeatMin"));
            int maxRepeat = Math.max(minRepeat, bi.tag.getInt("RepeatMax"));
            boolean stopAtSolid = bi.tag.getBoolean("StopAtSolid");
            NbtCompound dataTag = bi.tag.getCompound("Data");
            // the data should be valid, barring incompatible updates to the NBT schema
            Optional<StructureRepeaterBlockEntity.RepeaterData> optionalData = StructureRepeaterBlockEntity.RepeaterData.CODEC
                .parse(NbtOps.INSTANCE, dataTag)
                .result();
            if(optionalData.isPresent()) {
                BlockPos.Mutable pos = new BlockPos.Mutable();
                StructureRepeaterBlockEntity.RepeaterData data = optionalData.get();
                switch(data.mode) {
                    // repeat a single block state
                    case SINGLE: {
                        BlockState state = data.asSingle().state;
                        pos.set(bi.pos);
                        int repetitions = getSingleRepetitions(world, dir, minRepeat, maxRepeat, stopAtSolid, pos);
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
                // replace the repeater block
                BlockState replacement = parseBlockState(bi.tag.getString("Replacement"));
                world.setBlockState(bi.pos, replacement, 0);
            }
        }
    }

    /**
     * Compute the number of repetitions in a single-mode repeater.
     */
    private static int getSingleRepetitions(WorldAccess world, Direction dir, int min, int max, boolean stopAtSolid, BlockPos.Mutable pos) {
        if(stopAtSolid) {
            pos.move(dir, min + 1);
            int amt = min;
            while(amt < max && !world.getBlockState(pos).isOpaque()) {
                pos.move(dir);
                amt++;
            }
            return amt;
        } else {
            return world.getRandom().nextInt(max - min + 1) + min;
        }
    }
}
