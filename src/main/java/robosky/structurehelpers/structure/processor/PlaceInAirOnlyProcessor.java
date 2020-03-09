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
 * in air. Useful for structures that bore through the gound,
 * as mineshafts do.
 * TODO: Place in water / fluids.
 */
public class PlaceInAirOnlyProcessor extends StructureProcessor {

    private final List<BlockState> states;

    private PlaceInAirOnlyProcessor(List<BlockState> states) {
        this.states = states;
    }

    public static PlaceInAirOnlyProcessor create(BlockState... states) {
        return new PlaceInAirOnlyProcessor(ImmutableList.copyOf(states));
    }

    @Override
    public StructureBlockInfo process(WorldView world, BlockPos pos, StructureBlockInfo meh,
            StructureBlockInfo info, StructurePlacementData data) {
        if(states.contains(info.state)) {
            if(world.getBlockState(info.pos).isAir()) {
                return info;
            } else {
                return null;
            }
        }
        return info;
    }

    @Override
    protected StructureProcessorType getType() {
        return StructureHelpers.IN_AIR_ONLY_TYPE;
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

    public static PlaceInAirOnlyProcessor deserialize(Dynamic<?> dyn) {
        return new PlaceInAirOnlyProcessor(dyn.get("States")
            .asList(BlockState::deserialize));
    }
}
