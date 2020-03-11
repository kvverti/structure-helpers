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
 * Replaces only solid blocks, like Stronghold walls do
 */
public class OnlyInSolidProcessor extends StructureProcessor {

    private final List<BlockState> states;

    private OnlyInSolidProcessor(List<BlockState> states) {
        this.states = states;
    }

    public static OnlyInSolidProcessor create(BlockState... states) {
        return new OnlyInSolidProcessor(ImmutableList.copyOf(states));
    }

    @Override
    public StructureBlockInfo process(WorldView world, BlockPos pos, StructureBlockInfo meh,
            StructureBlockInfo info, StructurePlacementData data) {
        if(states.contains(info.state)) {
            if(world.getBlockState(info.pos).getCollisionShape(world, info.pos).isEmpty()) {
                return null;
            }
        }
        return info;
    }

    @Override
    protected StructureProcessorType getType() {
        return StructureHelpers.IN_SOLID_ONLY_TYPE;
    }

    @Override
    protected <T> Dynamic<T> rawToDynamic(DynamicOps<T> ops) {
        Map<T, T> map = ImmutableMap.of(
            ops.createString("States"),
            ops.createList(states.stream()
                .map(s -> BlockState.serialize(ops, s).getValue()))
        );
        return new Dynamic<>(ops, ops.createMap(map));
    }

    public static OnlyInSolidProcessor deserialize(Dynamic<?> dyn) {
        return new OnlyInSolidProcessor(dyn.get("States")
            .asList(BlockState::deserialize));
    }
}
