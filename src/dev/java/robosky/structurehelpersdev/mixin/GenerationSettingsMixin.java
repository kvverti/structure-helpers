package robosky.structurehelpersdev.mixin;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.biome.GenerationSettings;

@Mixin(GenerationSettings.class)
public abstract class GenerationSettingsMixin {

    @ModifyVariable(method = "<init>", ordinal = 1, at = @At(value = "LOAD"))
    private List<?> mutifyStructureList(List<?> value) {
        if(value instanceof ImmutableList<?>) {
            return new ArrayList<>(value);
        }
        return value;
    }
}
