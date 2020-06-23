package robosky.structurehelpers.structure.processor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

    public static final Codec<WeightedChanceProcessor> CODEC = RecordCodecBuilder.<Map.Entry<PartialBlockState, List<Entry>>>create(
        inst -> inst.group(
            PartialBlockState.CODEC.fieldOf("Key").forGetter(Map.Entry::getKey),
            Entry.CODEC.listOf().fieldOf("Replacements").forGetter(Map.Entry::getValue)
        ).apply(inst, AbstractMap.SimpleImmutableEntry::new))
        .listOf()
        .fieldOf("Entries")
        .xmap(es -> new WeightedChanceProcessor(ImmutableMap.copyOf(es)),
            proc -> ImmutableList.copyOf(proc.entries.entrySet()))
        .codec();

    private final Map<PartialBlockState, List<Entry>> entries;
    private final float weightSum;

    private WeightedChanceProcessor(Map<PartialBlockState, List<Entry>> entries) {
        this.entries = entries;
        weightSum = (float)entries.values().stream().flatMap(List::stream).mapToDouble(entry -> entry.weight).sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A record of a {@link PartialBlockState} with a weight.
     */
    public static final class Entry {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("Weight").forGetter(e -> e.weight),
            PartialBlockState.CODEC.fieldOf("Target").forGetter(e -> e.targetState)
        ).apply(inst, Entry::new));

        public final float weight;
        @Nullable
        public final PartialBlockState targetState;

        private Entry(float weight, @Nullable PartialBlockState targetState) {
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
    protected StructureProcessorType<WeightedChanceProcessor> getType() {
        return StructureHelpers.RANDOM_CHANCE_TYPE;
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
