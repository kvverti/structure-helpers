package robosky.structurehelpers.structure.processor;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;

/**
 * A representation of a set of {@link BlockState} matching a {@link Block} and {@link Property} values.
 * This is used to determine input and output block states for {@link WeightedChanceProcessor}.
 *
 * @see WeightedChanceProcessor
 * @see PartialBlockState.Builder
 */
public final class PartialBlockState {

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

    /**
     * Serializes this object to a {@link Dynamic}.
     */
    public <T> Dynamic<T> toDynamic(DynamicOps<T> ops) {
        return new Dynamic<>(ops, ops.createMap(
            ImmutableMap.of(
                ops.createString("Block"), ops.createString(Registry.BLOCK.getId(block).toString()),
                ops.createString("PropertyEntries"), ops.createList(
                    propertyEntries.entrySet()
                        .stream()
                        .map(e -> ops.createMap(
                            ImmutableMap.of(
                                ops.createString("Property"), ops.createString(e.getKey().getName()),
                                ops.createString("Value"), ops.createString(value(e.getKey(), e.getValue()))
                            )
                        ))
                )
            )
        ));
    }

    /**
     * Deserializes a {@code PartialBlockState} from a {@link Dynamic}.
     */
    public static PartialBlockState fromDynamic(Dynamic<?> dyn) {
        Block block;
        try {
            Identifier blockId = new Identifier(dyn.get("Block").asString(""));
            block = Registry.BLOCK.get(blockId);
        } catch(InvalidIdentifierException e) {
            block = null;
        }
        if(block == null) {
            block = Registry.BLOCK.get(Registry.BLOCK.getDefaultId());
        }
        Map<String, Property<?>> props = new HashMap<>();
        for(Property<?> prop : block.getDefaultState().getProperties()) {
            props.put(prop.getName(), prop);
        }
        Map<String, String> map = dyn.get("PropertyEntries").asMap(d -> d.asString(""), d -> d.asString(""));
        ImmutableMap.Builder<Property<?>, Comparable<?>> b = ImmutableMap.builder();
        for(Map.Entry<String, String> entry : map.entrySet()) {
            Property<?> key = props.get(entry.getKey());
            if(key != null) {
                key.parse(entry.getValue()).ifPresent(v -> b.put(key, v));
            }
        }
        return new PartialBlockState(block, b.build());
    }

    private static <T extends Comparable<T>> String value(Property<T> prop, Comparable<?> val) {
        return prop.name(prop.getType().cast(val));
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
}
