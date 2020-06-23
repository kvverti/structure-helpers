package robosky.structurehelpers.structure.processor;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.registry.Registry;

/**
 * A representation of a set of {@link BlockState} matching a {@link Block} and {@link Property} values.
 * This is used to determine input and output block states for {@link WeightedChanceProcessor}.
 *
 * @see WeightedChanceProcessor
 * @see PartialBlockState.Builder
 */
public final class PartialBlockState {

    public static final Codec<PartialBlockState> CODEC;

    /**
     * The {@link Block} instance to match.
     */
    private final Block block;

    /**
     * The set of {@link Property}s to match. These must be compatible with
     * the block.
     *
     * @see #block
     */
    private final ImmutableMap<Property<?>, Comparable<?>> propertyEntries;

    private PartialBlockState(Block block, ImmutableMap<Property<?>, Comparable<?>> propertyEntries) {
        this.block = block;
        this.propertyEntries = propertyEntries;
    }

    /**
     * Creates a new {@link PartialBlockState} for all states of the given block.
     */
    public static PartialBlockState of(Block block) {
        return new PartialBlockState(block, ImmutableMap.of());
    }

    /**
     * Creates a new {@link PartialBlockState} for the given block state.
     */
    public static PartialBlockState of(BlockState state) {
        return new PartialBlockState(state.getBlock(), state.getEntries());
    }

    /**
     * Creates a new builder for a {@code PartialBlockState} for the given block.
     */
    public static Builder builder(Block block) {
        return new Builder(block);
    }

    /**
     * Tests whether a {@link BlockState} is represented in this object.
     *
     * @param state The block state.
     */
    public boolean matches(BlockState state) {
        if(state.getBlock() == block) {
            for(Map.Entry<Property<?>, Comparable<?>> entry : propertyEntries.entrySet()) {
                if(!entry.getValue().equals(state.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Merges the block and block state properties defined in this object with the given block state.
     * Incompatible properties are dropped from the source block state.
     *
     * @param state The source block state.
     * @return A mapped block state represented in this object.
     */
    public BlockState map(BlockState state) {
        BlockState ret = block.getDefaultState();
        for(Map.Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet()) {
            if(ret.contains(entry.getKey())) {
                ret = withValue(ret, entry.getKey(), entry.getValue());
            }
        }
        for(Map.Entry<Property<?>, Comparable<?>> entry : propertyEntries.entrySet()) {
            ret = withValue(ret, entry.getKey(), entry.getValue());
        }
        return ret;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState in, Property<T> prop, Comparable<?> val) {
        return in.with(prop, prop.getType().cast(val));
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        PartialBlockState that = (PartialBlockState)o;
        if(!block.equals(that.block)) {
            return false;
        }
        return propertyEntries.equals(that.propertyEntries);
    }

    @Override
    public int hashCode() {
        int result = block.hashCode();
        result = 31 * result + propertyEntries.hashCode();
        return result;
    }

    /**
     * Builder for {@link PartialBlockState}.
     *
     * @see PartialBlockState
     */
    public static final class Builder {

        private final Block block;
        private final Map<Property<?>, Comparable<?>> propertyEntries;

        Builder(Block block) {
            this.block = block;
            this.propertyEntries = new HashMap<>();
        }

        /**
         * Adds A property-value pair to the pending {@link PartialBlockState}.
         *
         * @param property The property.
         * @param value    The value.
         * @return This builder.
         * @throws IllegalArgumentException If the given property or value is not allowed for the block.
         */
        public <T extends Comparable<T>> Builder with(Property<T> property, T value) {
            if(!block.getDefaultState().contains(property)) {
                throw new IllegalArgumentException(
                    "Invalid property " + property.getName() + " for block " + Registry.BLOCK.getId(block));
            }
            if(!property.getValues().contains(value)) {
                throw new IllegalArgumentException(
                    "Invalid value " + property.name(value) + " for property " + property.getName());
            }
            propertyEntries.put(property, value);
            return this;
        }

        /**
         * Builds and returns a new {@link PartialBlockState}.
         */
        public PartialBlockState build() {
            return new PartialBlockState(block, ImmutableMap.copyOf(propertyEntries));
        }
    }

    static {
        CODEC = Registry.BLOCK
            .dispatch(
                "Block",
                ps -> ps.block,
                b -> {
                    // store the possible property entry types
                    Map<String, Property<?>> props = new HashMap<>();
                    for(Property<?> prop : b.getDefaultState().getProperties()) {
                        props.put(prop.getName(), prop);
                    }
                    // property
                    return Codec.STRING
                        .<Property<?>>comapFlatMap(
                            n -> {
                                Property<?> p = props.get(n);
                                return p == null ? DataResult.error("Key: " + n) : DataResult.success(p);
                            },
                            Property::getName)
                        .dispatch(
                            "Property",
                            Map.Entry::getKey,
                            // value
                            p -> Codec.STRING
                                .<Map.Entry<Property<?>, Comparable<?>>>comapFlatMap(
                                    valueName -> p.parse(valueName)
                                        .map(v -> DataResult.success(
                                            new AbstractMap.SimpleEntry<Property<?>, Comparable<?>>(p, v)))
                                        .orElseGet(() -> DataResult.error("Value: " + valueName)),
                                    pr -> value(pr.getKey(), pr.getValue()))
                                .fieldOf("Value")
                                .codec()
                        )
                        .listOf()
                        .xmap(ls -> new PartialBlockState(b, ImmutableMap.copyOf(ls)),
                            ps -> ImmutableList.copyOf(ps.propertyEntries.entrySet()))
                        .fieldOf("PropertyEntries")
                        .codec();
                }
            );
    }

    private static <T extends Comparable<T>> String value(Property<T> prop, Comparable<?> val) {
        return prop.name(prop.getType().cast(val));
    }
}
