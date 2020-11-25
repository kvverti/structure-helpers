package robosky.structurehelpers;

import robosky.structurehelpers.block.LootDataBlock;
import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.network.ServerStructHelpPackets;
import robosky.structurehelpers.structure.pool.ExtendedSinglePoolElement;
import robosky.structurehelpers.structure.processor.AirGroundReplacementProcessor;
import robosky.structurehelpers.structure.processor.WeightedChanceProcessor;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;

public class StructureHelpers implements ModInitializer {

    public static final String MODID = "structure-helpers";

    public static final StructureProcessorType<WeightedChanceProcessor> RANDOM_CHANCE_TYPE = Registry.register(
        Registry.STRUCTURE_PROCESSOR,
        id("random_chance_processor"),
        () -> WeightedChanceProcessor.CODEC);

    public static final StructureProcessorType<AirGroundReplacementProcessor> AIR_GROUND_REPLACE_TYPE = Registry.register(
        Registry.STRUCTURE_PROCESSOR,
        id("air_ground_replacement_processor"),
        () -> AirGroundReplacementProcessor.CODEC);

    public static final Block LOOT_DATA_BLOCK = Registry.register(
        Registry.BLOCK,
        id("loot_data"),
        new LootDataBlock(Block.Settings.copy(Blocks.BEDROCK)));

    public static final BlockEntityType<LootDataBlockEntity> LOOT_DATA_ENTITY_TYPE =
        Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            id("loot_data"),
            BlockEntityType.Builder.create(LootDataBlockEntity::new, LOOT_DATA_BLOCK).build(null));

    public static Identifier id(String value) {
        return new Identifier(MODID, value);
    }

    @Override
    public void onInitialize() {
        ServerStructHelpPackets.init();
        Registry.register(Registry.ITEM,
            id("loot_data"),
            new BlockItem(LOOT_DATA_BLOCK, new Item.Settings().rarity(Rarity.EPIC)));
        Registry.register(Registry.STRUCTURE_POOL_ELEMENT,
            StructureHelpers.id("metadata_element"),
            ExtendedSinglePoolElement.TYPE);
    }
}
