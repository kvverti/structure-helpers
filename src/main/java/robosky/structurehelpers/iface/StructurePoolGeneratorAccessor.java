package robosky.structurehelpers.iface;

import java.util.Map;

import net.minecraft.util.Identifier;

/**
 * Accessor interface for {@link net.minecraft.structure.pool.StructurePoolBasedGenerator}.
 */
public interface StructurePoolGeneratorAccessor {

    /**
     * Sets the element placement range map for the current structure.
     *
     * @param elementMinMax A range map from element ID to range.
     */
    void structhelp_setRoomMinMax(Map<Identifier, ElementRange> elementMinMax);

    /**
     * Set the flag that this generator should start generating child junction elements.
     */
    void structhelp_setGeneratingChildren();

    /**
     * Checks that the range constraints of the generated structure are satisfied,
     * and warns if they are not.
     *
     * @return Whether the range constraints are satisfied.
     */
    boolean structhelp_softCheckMinMaxConstraints();
}
