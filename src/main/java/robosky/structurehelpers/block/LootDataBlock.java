package robosky.structurehelpers.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import robosky.structurehelpers.network.OpenLootDataS2CPacket;

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
        if (!player.isCreativeLevelTwoOp()) {
            return false;
        }
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof LootDataBlockEntity) {
                LootDataBlockEntity ld = (LootDataBlockEntity)be;
                OpenLootDataS2CPacket packet =
                    new OpenLootDataS2CPacket(pos, ld.getLootTable().toString(), ld.getReplacementState());
                ((ServerPlayerEntity)player).networkHandler.sendPacket(packet);
            }
        }
        return true;
    }
}
