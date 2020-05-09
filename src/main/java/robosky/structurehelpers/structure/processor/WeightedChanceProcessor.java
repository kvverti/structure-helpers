package robosky.structurehelpers.structure.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;

/**
 * Replaces given block with another randomly chosen from the given pool.
 */
public class WeightedChanceProcessor extends StructureProcessor {

    private final Map<PartialBlockState, List<Entry>> entries;
    private final float weightSum;

    private WeightedChanceProcessor(Map<PartialBlockState, List<Entry>> entries) {
        this.entries = entries;
        weightSum = (float)entries.values().stream().flatMap(List::stream).mapToDouble(entry -> entry.weight).sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Entry {
        public final float weight;
        /*@Nullable*/
        public final PartialBlockState targetState;

        private Entry(float weight, /*@Nullable*/ PartialBlockState targetState) {
            this.weight = weight;
            this.targetState = targetState;
        }

        public static Entry ofEmpty(float weight) {
            return of((PartialBlockState)null, weight);
        }

        public static Entry of(BlockState target, float weight) {
            return of(PartialBlockState.of(target), weight);
        }

        public static Entry of(Block target, float weight) {
            return of(PartialBlockState.of(target), weight);
        }

        public static Entry of(PartialBlockState target, float weight) {
            return new Entry(weight, target);
        }

        public <T> Dynamic<T> serialize(DynamicOps<T> ops) {
            ImmutableMap.Builder<T, T> b = ImmutableMap.builder();
            b.put(ops.createString("Weight"), ops.createFloat(weight));
            if(targetState != null) {
                b.put(ops.createString("Target"), targetState.toDynamic(ops).getValue());
            }
            return new Dynamic<>(ops, ops.createMap(b.build()));
        }

        public static Entry deserialize(Dynamic<?> dynamic) {
            float weight = dynamic.get("Weight").asFloat(0);
            PartialBlockState tgt = dynamic.get("Target").map(PartialBlockState::fromDynamic).orElse(null);
            return new Entry(weight, tgt);
        }
    }

    @Override
    public Structure.StructureBlockInfo process(
        WorldView world, BlockPos pos, BlockPos pos2, Structure.StructureBlockInfo thing,
        Structure.StructureBlockInfo info, StructurePlacementData data
    ) {
        Random rand = new Random(MathHelper.hashCode(info.pos));
        for(Map.Entry<PartialBlockState, List<Entry>> entry : entries.entrySet()) {
            PartialBlockState pbs = entry.getKey();
            if(pbs.matches(info.state)) {
                float totalWeight = 0f;
                float value = rand.nextFloat() * weightSum;
                for(Entry e : entry.getValue()) {
                    totalWeight += e.weight;
                    if(value < totalWeight) {
                        return e.targetState == null ? null
                            : new Structure.StructureBlockInfo(info.pos, e.targetState.map(info.state), null);
                    }
                }
                break;
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
        Stream<T> s = entries.entrySet().stream()
            .map(e -> ops.createMap(ImmutableMap.of(
                ops.createString("Key"), e.getKey().toDynamic(ops).getValue(),
                ops.createString("Replacements"), ops.createList(e.getValue()
                    .stream().map(e2 -> e2.serialize(ops).getValue()))
            )));
        return new Dynamic<>(ops, ops.createMap(ImmutableMap.of(
            ops.createString("Entries"), ops.createList(s)
        )));
    }

    public static WeightedChanceProcessor deserialize(Dynamic<?> dyn) {
        Map<PartialBlockState, List<Entry>> entries = dyn.get("Entries")
            .asList(Function.identity())
            .stream()
            .collect(Collectors.toMap(
                dy -> dy.get("Key").map(PartialBlockState::fromDynamic).orElse(PartialBlockState.of(Blocks.AIR)),
                dy -> dy.get("Replacements").asList(Entry::deserialize)
            ));
        return new WeightedChanceProcessor(entries);
    }

    /**
     * Builder for {@link WeightedChanceProcessor}.
     */
    public static final class Builder {

        private final Map<PartialBlockState, List<Entry>> entryMap = new HashMap<>();

        private Builder() {
        }

        public Builder add(PartialBlockState state, Entry... entries) {
            entryMap.computeIfAbsent(state, k -> new ArrayList<>())
                .addAll(Arrays.asList(entries));
            return this;
        }

        public Builder add(BlockState state, Entry... entries) {
            return add(PartialBlockState.of(state), entries);
        }

        public Builder add(Block block, Entry... entries) {
            return add(PartialBlockState.of(block), entries);
        }

        public WeightedChanceProcessor build() {
            Map<PartialBlockState, List<Entry>> map = ImmutableMap.copyOf(entryMap);
            entryMap.clear(); // prevent leakage in case of builder reuse
            return new WeightedChanceProcessor(map);
        }
    }
}