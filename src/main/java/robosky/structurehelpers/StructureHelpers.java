package robosky.structurehelpers;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.fabricmc.api.ModInitializer;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;

import robosky.structurehelpers.block.LootDataBlock;
import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.network.ServerStructHelpPackets;
import robosky.structurehelpers.structure.processor.*;

public class StructureHelpers implements ModInitializer {

    public static final String MODID = "structure-helpers";

    public static final StructureProcessorType RANDOM_CHANCE_TYPE = Registry.register(
        Registry.STRUCTURE_PROCESSOR,
        new Identifier(MODID, "random_chance_processor"),
        RandomChanceProcessor::deserialize);

    public static final StructureProcessorType AIR_GROUND_REPLACE_TYPE = Registry.register(
        Registry.STRUCTURE_PROCESSOR,
        new Identifier(MODID, "air_ground_replacement_processor"),
        AirGroundReplacementProcessor::deserialize);

    public static final Block LOOT_DATA_BLOCK = Registry.register(
        Registry.BLOCK,
        new Identifier(MODID, "loot_data"),
        new LootDataBlock(Block.Settings.copy(Blocks.BEDROCK)));

    public static final Item LOOT_DATA_ITEM = Registry.register(
        Registry.ITEM,
        new Identifier(MODID, "loot_data"),
        new BlockItem(LOOT_DATA_BLOCK, new Item.Settings().rarity(Rarity.EPIC)));

    public static final BlockEntityType<LootDataBlockEntity> LOOT_DATA_ENTITY_TYPE =
        Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new Identifier(MODID, "loot_data"),
            BlockEntityType.Builder.create(LootDataBlockEntity::new, LOOT_DATA_BLOCK).build(null));

    public static final Identifier id(String value) {
        return new Identifier(MODID, value);
    }

    @Override
    public void onInitialize() {
        ServerStructHelpPackets.init();
    }
}
