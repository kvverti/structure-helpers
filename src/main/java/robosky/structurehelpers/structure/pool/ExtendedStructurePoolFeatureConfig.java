package robosky.structurehelpers.structure.pool;

import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.FeatureConfig;

/**
 * A {@link FeatureConfig} for extended structure pool features. This config defines structure extents and
 * range constraints.
 */
public class ExtendedStructurePoolFeatureConfig implements FeatureConfig {

    public static final Codec<ExtendedStructurePoolFeatureConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        ElementRange.CODEC.listOf()
            .xmap(ImmutableList::copyOf, Function.identity())
            .fieldOf("RangeConstraints")
            .forGetter(c -> c.rangeConstraints),
        Codec.INT.fieldOf("HorizontalExtent").forGetter(c -> c.horizontalExtent),
        Codec.INT.fieldOf("VerticalExtent").forGetter(c -> c.verticalExtent),
        Identifier.CODEC.fieldOf("StartPool").forGetter(c -> c.startPool),
        Codec.INT.fieldOf("Size").forGetter(c -> c.size)
    ).apply(inst, ExtendedStructurePoolFeatureConfig::new));

    public final ImmutableList<ElementRange> rangeConstraints;
    public final int horizontalExtent;
    public final int verticalExtent;
    public final Identifier startPool;
    public final int size;

    public ExtendedStructurePoolFeatureConfig(
        ImmutableList<ElementRange> rangeConstraints,
        int horizontalExtent,
        int verticalExtent,
        Identifier startPool,
        int size
    ) {
        this.rangeConstraints = rangeConstraints;
        this.horizontalExtent = horizontalExtent;
        this.verticalExtent = verticalExtent;
        this.startPool = startPool;
        this.size = size;
    }
}
