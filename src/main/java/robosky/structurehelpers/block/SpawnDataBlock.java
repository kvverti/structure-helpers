package robosky.structurehelpers.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SpawnDataBlock extends Block implements BlockEntityProvider {

    public SpawnDataBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new SpawnDataBlockEntity();
    }

    @Override
    public ActionResult onUse(
        BlockState state,
        World world,
        BlockPos pos,
        PlayerEntity player,
        Hand hand,
        BlockHitResult ctx
    ) {
        if(!player.isCreativeLevelTwoOp()) {
            return ActionResult.PASS;
        }
//        if(!world.isClient()) {
//            BlockEntity be = world.getBlockEntity(pos);
//            if(be instanceof SpawnDataBlockEntity) {
//                SpawnDataBlockEntity ld = (SpawnDataBlockEntity)be;
//                LootDataPacketData data =
//                    new LootDataPacketData(pos, ld.getLootTable().toString(), ld.getReplacementState());
//                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//                data.write(buf);
//                ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ClientStructHelpPackets.LOOT_DATA_OPEN, buf);
//            }
//        }
        return ActionResult.SUCCESS;
    }
}
