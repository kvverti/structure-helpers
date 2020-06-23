package robosky.structurehelpersdev.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.StructureFeature;

@Mixin(StructureFeature.class)
public interface StructureFeatureAccess {

    @Invoker
    static <F extends StructureFeature<?>> F callRegister(String name, F structureFeature, GenerationStep.Feature step) {
        throw new AssertionError();
    }
}
