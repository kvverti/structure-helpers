package robosky.structurehelpers.structure.pool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

/**
 * A data class used to pass pool element placement number data
 * to the jigsaw structure pool element generator.
 */
public final class ElementRange {

    public static final Codec<ElementRange> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Identifier.CODEC.fieldOf("Id").forGetter(ElementRange::id),
        Codec.INT.fieldOf("Min").forGetter(ElementRange::min),
        Codec.INT.fieldOf("Max").forGetter(ElementRange::max)
    ).apply(inst, ElementRange::new));

    /**
     * @deprecated This class will become a record class in the next major release. Use {@link #id()} instead.
     */
    @Deprecated(since = "3.1.1", forRemoval = true)
    public final Identifier id;

    /**
     * @deprecated This class will become a record class in the next major release. Use {@link #min()} instead.
     */
    @Deprecated(since = "3.1.1", forRemoval = true)
    public final int min;

    /**
     * @deprecated This class will become a record class in the next major release. Use {@link #max()} instead.
     */
    @Deprecated(since = "3.1.1", forRemoval = true)
    public final int max;

    private ElementRange(Identifier id, int min, int max) {
        if(min < 0 || min > max) {
            throw new IllegalArgumentException("Invalid min-max range");
        }
        this.id = id;
        this.min = min;
        this.max = max;
    }

    public Identifier id() {
        return id;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    /**
     * Creates an ElementRange for the structure pool element with
     * the given location, with the given inclusive range.
     *
     * @param id  the location of the structure pool element
     * @param min the minimum number of times to generate the element, inclusive
     * @param max the maximum number of times to generate the element, inclusive
     */
    public static ElementRange of(Identifier id, int min, int max) {
        return new ElementRange(id, min, max);
    }
}
