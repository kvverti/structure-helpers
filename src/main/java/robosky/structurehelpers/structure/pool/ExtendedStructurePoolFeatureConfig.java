package robosky.structurehelpers.structure.pool;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.structure.pool.StructurePool;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;

/**
 * A {@link FeatureConfig} for extended structure pool features. This config defines structure extents and
 * range constraints.
 */
public class ExtendedStructurePoolFeatureConfig extends StructurePoolFeatureConfig implements FeatureConfig {

    public static final Codec<ExtendedStructurePoolFeatureConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        ElementRange.CODEC.listOf()
            .xmap(ImmutableList::copyOf, Function.identity())
            .fieldOf("RangeConstraints")
            .forGetter(c -> c.rangeConstraints),
        Codec.INT.fieldOf("HorizontalExtent").forGetter(c -> c.horizontalExtent),
        Codec.INT.fieldOf("VerticalExtent").forGetter(c -> c.verticalExtent),
        StructurePool.REGISTRY_CODEC.fieldOf("StartPool").forGetter(StructurePoolFeatureConfig::getStartPool),
        Codec.INT.fieldOf("Size").forGetter(StructurePoolFeatureConfig::getSize)
    ).apply(inst, ExtendedStructurePoolFeatureConfig::new));

    private final ImmutableList<ElementRange> rangeConstraints;
    private final int horizontalExtent;
    private final int verticalExtent;

    public ExtendedStructurePoolFeatureConfig(
        ImmutableList<ElementRange> rangeConstraints,
        int horizontalExtent,
        int verticalExtent,
        Supplier<StructurePool> startPool,
        int size
    ) {
        super(startPool, size);
        this.rangeConstraints = rangeConstraints;
        this.horizontalExtent = horizontalExtent;
        this.verticalExtent = verticalExtent;
    }

    public final ImmutableList<ElementRange> getRangeConstraints() {
        return rangeConstraints;
    }

    public final int getHorizontalExtent() {
        return horizontalExtent;
    }

    public final int getVerticalExtent() {
        return verticalExtent;
    }
}
