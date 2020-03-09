package robosky.structurehelpers.structure.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;

import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.structure.Structure.StructureBlockInfo;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import robosky.structurehelpers.StructureHelpers;

/**
 * A structure processor that only places the given BlockStates
 * if it would not replace air. Useful to avoid breaking the surface
 * or caves, as vanilla stringholds do.
 * TODO: Do not place in water / fluids
 */
public class PlaceInGroundOnlyProcessor extends StructureProcessor {

    private final List<BlockState> states;

    private PlaceInGroundOnlyProcessor(List<BlockState> states) {
        this.states = states;
    }

    public static PlaceInGroundOnlyProcessor create(BlockState... states) {
        return new PlaceInGroundOnlyProcessor(ImmutableList.copyOf(states));
    }

    @Override
    public StructureBlockInfo process(WorldView world, BlockPos pos, StructureBlockInfo meh,
            StructureBlockInfo info, StructurePlacementData data) {
        if(states.contains(info.state)) {
            if(world.getBlockState(info.pos).isAir()) {
                return null;
            }
        }
        return info;
    }

    @Override
    protected StructureProcessorType getType() {
        return StructureHelpers.IN_GROUND_ONLY_TYPE;
    }

    @Override
    protected <T> Dynamic<T> method_16666(DynamicOps<T> ops) {
        Map<T, T> map = ImmutableMap.of(
            ops.createString("States"),
            ops.createList(states.stream()
                .map(s -> BlockState.serialize(ops, s).getValue()))
        );
        return new Dynamic<>(ops, ops.createMap(map));
    }

    public static PlaceInGroundOnlyProcessor deserialize(Dynamic<?> dyn) {
        return new PlaceInGroundOnlyProcessor(dyn.get("States")
            .asList(BlockState::deserialize));
    }
}
