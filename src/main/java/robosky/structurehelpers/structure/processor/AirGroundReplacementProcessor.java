package robosky.structurehelpers.structure.processor;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

/**
 * A structure processor that only replaces certain block states with one
 * of two block states depending on whether it is placed in the air or
 * in the ground.
 */
public class AirGroundReplacementProcessor extends StructureProcessor {

    public static final Codec<AirGroundReplacementProcessor> CODEC = Entry.CODEC
        .listOf()
        .xmap(ls -> new AirGroundReplacementProcessor(ImmutableList.copyOf(ls)), proc -> proc.entries)
        .fieldOf("Entries")
        .codec();

    private final ImmutableList<Entry> entries;

    private AirGroundReplacementProcessor(ImmutableList<Entry> entries) {
        this.entries = entries;
    }

    /**
     * Creates a new processor with the given entries.
     */
    public static AirGroundReplacementProcessor create(Entry... entries) {
        return new AirGroundReplacementProcessor(ImmutableList.copyOf(entries));
    }

    @Override
    public StructureBlockInfo process(
        WorldView world, BlockPos pos, BlockPos pos2, StructureBlockInfo meh,
        StructureBlockInfo info, StructurePlacementData data
    ) {
        for(Entry entry : entries) {
            if(entry.key.matches(info.state)) {
                BlockState state;
                if(world.getBlockState(info.pos).getCollisionShape(world, info.pos).isEmpty()) {
                    state = entry.air == null ? null : entry.air.map(info.state);
                } else {
                    state = entry.ground == null ? null : entry.ground.map(info.state);
                }
                return state == null ? null : new StructureBlockInfo(info.pos, state, null);
            }
        }
        return info;
    }

    @Override
    protected StructureProcessorType<AirGroundReplacementProcessor> getType() {
        return StructureHelpers.AIR_GROUND_REPLACE_TYPE;
    }

    /**
     * Replacement entry. A null air or ground value indicates not to place
     * a block under that condition.
     */
    public static final class Entry {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            PartialBlockState.CODEC.fieldOf("Key").forGetter(e -> e.key),
            PartialBlockState.CODEC.optionalFieldOf("Air", null).forGetter(e -> e.air),
            PartialBlockState.CODEC.optionalFieldOf("Ground", null).forGetter(e -> e.ground)
        ).apply(inst, Entry::new));

        public final PartialBlockState key;
        /*@Nullable*/
        public final PartialBlockState air;
        /*@Nullable*/
        public final PartialBlockState ground;

        private Entry(
            PartialBlockState key,
            /*@Nullable*/ PartialBlockState air,
            /*@Nullable*/ PartialBlockState ground
        ) {
            this.key = key;
            this.air = air;
            this.ground = ground;
        }

        /**
         * Creates an entry for replacing the given key state with the given states in the air and ground.
         *
         * @param key    The block states to match.
         * @param air    The block states to place when replacing non-collidable blocks.
         * @param ground The block states to place when replacing collidable blocks.
         * @return The created Entry.
         * @see PartialBlockState
         * @see #of(Block, Block, Block)
         */
        public static Entry of(
            PartialBlockState key,
            /*@Nullable*/ PartialBlockState air,
            /*@Nullable*/ PartialBlockState ground
        ) {
            return new Entry(key, air, ground);
        }

        /**
         * Creates an entry for replacing the given block with the given blocks in the air and ground.
         * This method is a convenience for {@link #of(PartialBlockState, PartialBlockState, PartialBlockState)}.
         *
         * @param key    The block to match.
         * @param air    The block to place when replacing non-collidable blocks.
         * @param ground The block to place when replacing collidable blocks.
         * @return The created Entry.
         * @see #of(PartialBlockState, PartialBlockState, PartialBlockState)
         */
        public static Entry of(Block key, /*@Nullable*/ Block air, /*@Nullable*/ Block ground) {
            return of(PartialBlockState.of(key), PartialBlockState.of(air), PartialBlockState.of(ground));
        }

        /**
         * Creates an entry for placing the given state unchanged in air, and ignoring in the ground.
         */
        public static Entry airOnly(PartialBlockState key) {
            return of(key, key, null);
        }

        /**
         * Creates an entry for placing the given state unchanged in air, and ignoring in the ground.
         *
         * @see #airOnly(PartialBlockState)
         */
        public static Entry airOnly(Block key) {
            return airOnly(PartialBlockState.of(key));
        }

        /**
         * Creates an entry for placing the given state unchanged in the ground, and ignoring in air.
         */
        public static Entry groundOnly(PartialBlockState key) {
            return new Entry(key, null, key);
        }

        /**
         * Creates an entry for placing the given state unchanged in the ground, and ignoring in air.
         *
         * @see #groundOnly(PartialBlockState)
         */
        public static Entry groundOnly(Block key) {
            return groundOnly(PartialBlockState.of(key));
        }
    }
}
