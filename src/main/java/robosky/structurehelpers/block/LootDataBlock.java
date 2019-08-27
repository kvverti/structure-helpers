package robosky.structurehelpers.block;

import robosky.structurehelpers.client.LootDataScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import robosky.structurehelpers.iface.PlayerProxy;

public class LootDataBlock extends Block implements BlockEntityProvider {

    public LootDataBlock(Block.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new LootDataBlockEntity();
    }

    @Override
    public boolean activate(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult ctx) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof LootDataBlockEntity) {
            // ((PlayerProxy)player).structhelp_openLootDataBlock((LootDataBlockEntity)be);
            // temporary
            MinecraftClient.getInstance().openScreen(new LootDataScreen((LootDataBlockEntity)be));
        }
        return true;
    }
}
