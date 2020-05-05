package robosky.structurehelpers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.structure.StructurePlacementData;

/**
 * The setter for this property was likely stripped by Mojang's obfuscation.
 */
@Mixin(StructurePlacementData.class)
public interface StructurePlacementDataAccessor {

    @Accessor
    void setPlaceFluids(boolean place);
}
