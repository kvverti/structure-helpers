package robosky.structurehelpers.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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
}
