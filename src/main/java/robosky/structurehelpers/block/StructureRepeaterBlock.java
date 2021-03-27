package robosky.structurehelpers.block;

import io.netty.buffer.Unpooled;
import robosky.structurehelpers.network.ClientStructHelpPackets;
import robosky.structurehelpers.network.RepeaterPacketData;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class StructureRepeaterBlock extends Block implements BlockEntityProvider {

    public static final Property<Direction> FACING = EnumProperty.of("facing", Direction.class, Direction.UP, Direction.DOWN);

    public StructureRepeaterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StructureRepeaterBlockEntity(pos, state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction dir = ctx.getVerticalPlayerLookDirection().getOpposite();
        return this.getDefaultState().with(FACING, dir);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!player.isCreativeLevelTwoOp()) {
            return ActionResult.PASS;
        }
        if(!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if(be instanceof StructureRepeaterBlockEntity && player instanceof ServerPlayerEntity) {
                StructureRepeaterBlockEntity repeater = (StructureRepeaterBlockEntity)be;
                RepeaterPacketData data = new RepeaterPacketData();
                data.readFromBlockEntity(repeater);
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                data.write(buf);
                ServerPlayNetworking.send((ServerPlayerEntity)player, ClientStructHelpPackets.REPEATER_OPEN, buf);
            }
        }
        return ActionResult.SUCCESS;
    }
}
