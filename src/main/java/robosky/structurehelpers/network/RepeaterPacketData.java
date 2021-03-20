package robosky.structurehelpers.network;

import robosky.structurehelpers.block.StructureRepeaterBlockEntity;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public final class RepeaterPacketData {

    private BlockPos pos;
    private int minRepeat;
    private int maxRepeat;
    private boolean stopAtSolid;
    private StructureRepeaterBlockEntity.Mode mode;
    private String modeState;

    public RepeaterPacketData() {
        this(BlockPos.ORIGIN, 1, 1, false, StructureRepeaterBlockEntity.Mode.SINGLE, "minecraft:air");
    }

    public RepeaterPacketData(BlockPos pos, int minRepeat, int maxRepeat, boolean stopAtSolid, StructureRepeaterBlockEntity.Mode mode, String modeState) {
        this.pos = pos;
        this.minRepeat = minRepeat;
        this.maxRepeat = maxRepeat;
        this.stopAtSolid = stopAtSolid;
        this.mode = mode;
        this.modeState = modeState;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getMinRepeat() {
        return minRepeat;
    }

    public int getMaxRepeat() {
        return maxRepeat;
    }

    public boolean isStopAtSolid() {
        return stopAtSolid;
    }

    public StructureRepeaterBlockEntity.Mode getMode() {
        return mode;
    }

    public String getModeState() {
        return modeState;
    }

    public void read(PacketByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.minRepeat = buf.readInt();
        this.maxRepeat = buf.readInt();
        this.stopAtSolid = buf.readBoolean();
        this.mode = buf.readEnumConstant(StructureRepeaterBlockEntity.Mode.class);
        this.modeState = buf.readString();
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.minRepeat);
        buf.writeInt(this.maxRepeat);
        buf.writeBoolean(this.stopAtSolid);
        buf.writeEnumConstant(this.mode);
        buf.writeString(this.modeState);
    }
}
