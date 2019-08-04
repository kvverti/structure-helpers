package robosky.structurehelpers;

import net.fabricmc.api.ModInitializer;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class StructureHelpers implements ModInitializer {
    public static StructureProcessorType RANDOM_CHANCE_TYPE = Registry.register(Registry.STRUCTURE_PROCESSOR,
            new Identifier("structure-helpers", "random-chance-processor"), RandomChanceProcessor::deserialize);

    @Override
    public void onInitialize() {

    }
}
