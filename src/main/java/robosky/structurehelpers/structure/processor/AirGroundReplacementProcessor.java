package robosky.structurehelpers.structure.processor;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
    protected StructureProcessorType getType() {
        return StructureHelpers.AIR_GROUND_REPLACE_TYPE;
    }

    @Override
    protected <T> Dynamic<T> rawToDynamic(DynamicOps<T> ops) {
        Stream<T> entries = this.entries.stream()
            .map(e -> e.serialize(ops).getValue());
        return new Dynamic<>(ops, ops.createMap(ImmutableMap.of(
            ops.createString("Entries"), ops.createList(entries)
        )));
    }

    public static AirGroundReplacementProcessor deserialize(Dynamic<?> dyn) {
        List<Entry> entries = dyn.get("Entries").asList(Entry::deserialize);
        return new AirGroundReplacementProcessor(ImmutableList.copyOf(entries));
    }

    /**
     * Replacement entry. A null air or ground value indicates not to place
     * a block under that condition.
     */
    public static final class Entry {
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

        public <T> Dynamic<T> serialize(DynamicOps<T> ops) {
            ImmutableMap.Builder<T, T> states = ImmutableMap.builder();
            states.put(ops.createString("Key"), key.toDynamic(ops).getValue());
            if(air != null) {
                states.put(ops.createString("Air"), air.toDynamic(ops).getValue());
            }
            if(ground != null) {
                states.put(ops.createString("Ground"), ground.toDynamic(ops).getValue());
            }
            return new Dynamic<>(ops, ops.createMap(states.build()));
        }

        public static Entry deserialize(Dynamic<?> dyn) {
            return new Entry(
                dyn.get("Key").map(PartialBlockState::fromDynamic).orElse(PartialBlockState.of(Blocks.VOID_AIR)),
                dyn.get("Air").map(PartialBlockState::fromDynamic).orElse(null),
                dyn.get("Ground").map(PartialBlockState::fromDynamic).orElse(null)
            );
        }
    }
}
