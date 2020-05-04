package robosky.structurehelpers.structure.processor;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import robosky.structurehelpers.StructureHelpers;

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

    private static final Logger logger = LogManager.getLogger(AirGroundReplacementProcessor.class);

    private final Map<BlockState, Entry> stateMap;

    private AirGroundReplacementProcessor(List<Entry> entries) {
        this.stateMap = entries.stream().collect(toMap(e -> e.key, e -> e));
    }

    public static AirGroundReplacementProcessor create(Entry... entries) {
        return new AirGroundReplacementProcessor(Arrays.asList(entries));
    }

    @Override
    public StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos pos2, StructureBlockInfo meh,
            StructureBlockInfo info, StructurePlacementData data) {
        if(stateMap.containsKey(info.state)) {
            BlockState state;
            if(world.getBlockState(info.pos).getCollisionShape(world, info.pos).isEmpty()) {
                state = stateMap.get(info.state).air;
            } else {
                state = stateMap.get(info.state).ground;
            }
            return state == null ? null : new StructureBlockInfo(info.pos, state, null);
        }
        return info;
    }

    @Override
    protected StructureProcessorType getType() {
        return StructureHelpers.AIR_GROUND_REPLACE_TYPE;
    }

    @Override
    protected <T> Dynamic<T> rawToDynamic(DynamicOps<T> ops) {
        Stream<T> entries = stateMap.values().stream()
            .map(e -> e.serialize(ops).getValue());
        return new Dynamic<>(ops, ops.createMap(ImmutableMap.of(
            ops.createString("Entries"), ops.createList(entries)
        )));
    }

    public static AirGroundReplacementProcessor deserialize(Dynamic<?> dyn) {
        List<Entry> entries = dyn.get("Entries").asList(Entry::deserialize);
        return new AirGroundReplacementProcessor(entries);
    }

    /**
     * Replacement entry. A null air or ground value indicates not to place
     * a block under that condition.
     */
    public static final class Entry {
        public final BlockState key;
        /*@Nullable*/
        public final BlockState air;
        /*@Nullable*/
        public final BlockState ground;

        private Entry(BlockState key, /*@Nullable*/ BlockState air, /*@Nullable*/ BlockState ground) {
            this.key = key;
            this.air = air;
            this.ground = ground;
        }

        /**
         * Creates an entry for replacing the given key state with the given states
         * in air and in the ground.
         */
        public static Entry of(BlockState key, /*@Nullable*/ BlockState air, /*@Nullable*/ BlockState ground) {
            return new Entry(key, air, ground);
        }

        /**
         * Creates an entry for placing the given state only in air.
         */
        public static Entry airOnly(BlockState key) {
            return new Entry(key, key, null);
        }

        /**
         * Creates an entry for placing the given state only in the ground.
         */
        public static Entry groundOnly(BlockState key) {
            return new Entry(key, null, key);
        }

        public <T> Dynamic<T> serialize(DynamicOps<T> ops) {
            ImmutableMap.Builder<T, T> states = ImmutableMap.builder();
            states.put(ops.createString("Key"), BlockState.serialize(ops, key).getValue());
            if(air != null) {
                states.put(ops.createString("Air"), BlockState.serialize(ops, air).getValue());
            }
            if(ground != null) {
                states.put(ops.createString("Ground"), BlockState.serialize(ops, ground).getValue());
            }
            return new Dynamic<>(ops, ops.createMap(states.build()));
        }

        public static Entry deserialize(Dynamic<?> dyn) {
            BlockState key = dyn.get("Key").map(BlockState::deserialize).orElse(null);
            if(key == null) {
                logger.warn("Unknown block state key in air/ground processor");
                key = Blocks.VOID_AIR.getDefaultState();
            }
            return new Entry(
                key,
                dyn.get("Air").map(BlockState::deserialize).orElse(null),
                dyn.get("Ground").map(BlockState::deserialize).orElse(null)
            );
        }
    }
}
