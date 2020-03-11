package robosky.structurehelpers.structure.processor;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import robosky.structurehelpers.StructureHelpers;

/**
 * Replaces given block with another randomly chosen from the given pool.
 */
public class RandomChanceProcessor extends StructureProcessor {
    private final Map<BlockState, List<Entry>> entries;
    private final float weightSum;

    private RandomChanceProcessor(Map<BlockState, List<Entry>> entries) {
        this.entries = entries;
        weightSum = (float) entries.values().stream().flatMap(List::stream).mapToDouble(entry -> entry.weight).sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Entry {
        public final float weight;
        public final BlockState targetState;

        private Entry(float weight, BlockState targetState) {
            this.weight = weight;
            this.targetState = targetState;
        }

        public static Entry of(BlockState target, float weight) {
            return new Entry(weight, target);
        }

        public static Entry of(Block target, float weight) {
            return of(target.getDefaultState(), weight);
        }

        public <T> Dynamic<T> serialize(DynamicOps<T> ops) {
            return new Dynamic<>(ops, ops.createMap(ImmutableMap.of(ops.createString("Weight"), ops.createFloat(weight),
                    ops.createString("Target"), BlockState.serialize(ops, targetState).getValue())));
        }

        public static Entry deserialize(Dynamic<?> dynamic) {
            float weight = dynamic.get("Weight").asFloat(0);
            BlockState tgt = BlockState.deserialize(dynamic.get("Target").orElseEmptyMap());
            return new Entry(weight, tgt);
        }
    }

    @Override
    public Structure.StructureBlockInfo process(WorldView world, BlockPos pos, Structure.StructureBlockInfo thing,
                                                Structure.StructureBlockInfo info, StructurePlacementData data) {
        Random rand = new Random(MathHelper.hashCode(info.pos));
        if (entries.containsKey(info.state)) {
            float totalWeight = 0f;
            float value = rand.nextFloat() * weightSum;
            for (Entry entry : entries.get(info.state)) {
                totalWeight += entry.weight;
                if (value < totalWeight) {
                    return new Structure.StructureBlockInfo(info.pos, entry.targetState, null);
                }
            }
        }
        return info;
    }

    @Override
    protected StructureProcessorType getType() {
        return StructureHelpers.RANDOM_CHANCE_TYPE;
    }

    @Override
    protected <T> Dynamic<T> rawToDynamic(DynamicOps<T> ops) {
        Map<T, T> map = entries.entrySet().stream().collect(Collectors.toMap(entry -> BlockState.serialize(ops, entry
                .getKey()).getValue(), entry -> ops.createList(entry.getValue().stream().map(e -> e.serialize(ops)
                .getValue())), (a, b) -> b));
        return new Dynamic<>(ops, ops.createMap(ImmutableMap.of(ops.createString("Entries"), ops.createMap(map))));
    }

    public static RandomChanceProcessor deserialize(Dynamic<?> dynamic) {
        return new RandomChanceProcessor(dynamic.get("Entries").asMap(BlockState::deserialize, dyn ->
                dyn.asList(Entry::deserialize)));
    }

    /**
     * Builder for {@link RandomChanceProcessor}.
     */
    public static final class Builder {

        private final Map<BlockState, List<Entry>> entryMap = new HashMap<>();

        private Builder() {}

        public Builder add(BlockState state, Entry... entries) {
            entryMap.computeIfAbsent(state, k -> new ArrayList<>())
                .addAll(Arrays.asList(entries));
            return this;
        }

        public Builder add(Block block, Entry... entries) {
            return add(block.getDefaultState(), entries);
        }

        public RandomChanceProcessor build() {
            Map<BlockState, List<Entry>> map = ImmutableMap.copyOf(entryMap);
            entryMap.clear(); // prevent leakage in case of builder reuse
            return new RandomChanceProcessor(map);
        }
    }
}
