package robosky.structurehelpers.structure.pool;

import net.minecraft.util.Identifier;

/**
 * A data class used to pass pool element placement number data
 * to the jigsaw structure pool element generator.
 */
public final class ElementRange {

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
