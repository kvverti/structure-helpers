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
        Identifier.CODEC.fieldOf("Id").forGetter(r -> r.id),
        Codec.INT.fieldOf("Min").forGetter(r -> r.min),
        Codec.INT.fieldOf("Max").forGetter(r -> r.max)
    ).apply(inst, ElementRange::of));

    public final Identifier id;
    public final int min;
    public final int max;

    private ElementRange(Identifier id, int min, int max) {
        this.id = id;
        this.min = min;
        this.max = max;
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
        if(min < 0 || min > max) {
            throw new IllegalArgumentException("Invalid min-max range");
        }
        return new ElementRange(id, min, max);
    }
}
