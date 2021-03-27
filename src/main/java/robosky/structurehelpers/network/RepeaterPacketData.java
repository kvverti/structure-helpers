package robosky.structurehelpers.network;

import robosky.structurehelpers.block.StructureRepeaterBlockEntity;
import robosky.structurehelpers.structure.ExtendedStructureHandling;

import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class RepeaterPacketData {

    private BlockPos pos;
    private int minRepeat;
    private int maxRepeat;
    private boolean stopAtSolid;
    private String replacement;
    private StructureRepeaterBlockEntity.Mode mode;
    private String data1;
    private String data2;
    private String data3;

    public RepeaterPacketData() {
        this(BlockPos.ORIGIN, 1, 1, false, "minecraft:air", StructureRepeaterBlockEntity.Mode.SINGLE, "minecraft:air", "", "");
    }

    public RepeaterPacketData(BlockPos pos,
                              int minRepeat,
                              int maxRepeat,
                              boolean stopAtSolid,
                              String replacement,
                              StructureRepeaterBlockEntity.Mode mode,
                              String data1,
                              String data2,
                              String data3) {
        this.pos = pos;
        this.minRepeat = minRepeat;
        this.maxRepeat = maxRepeat;
        this.stopAtSolid = stopAtSolid;
        this.replacement = replacement;
        this.mode = mode;
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void read(PacketByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.minRepeat = buf.readInt();
        this.maxRepeat = buf.readInt();
        this.stopAtSolid = buf.readBoolean();
        this.replacement = buf.readString();
        this.mode = buf.readEnumConstant(StructureRepeaterBlockEntity.Mode.class);
        this.data1 = buf.readString();
        this.data2 = buf.readString();
        this.data3 = buf.readString();
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.minRepeat);
        buf.writeInt(this.maxRepeat);
        buf.writeBoolean(this.stopAtSolid);
        buf.writeString(this.replacement);
        buf.writeEnumConstant(this.mode);
        buf.writeString(this.data1);
        buf.writeString(this.data2);
        buf.writeString(this.data3);
    }

    public void readFromBlockEntity(StructureRepeaterBlockEntity be) {
        this.pos = be.getPos();
        this.minRepeat = be.getMinRepeat();
        this.maxRepeat = be.getMaxRepeat();
        this.stopAtSolid = be.stopsAtSolid();
        this.replacement = ExtendedStructureHandling.stringifyBlockState(be.getReplacementState());
        this.mode = be.getMode();
        switch(be.getMode()) {
            case SINGLE: {
                StructureRepeaterBlockEntity.Single single = be.getData().asSingle();
                this.data1 = ExtendedStructureHandling.stringifyBlockState(single.fill);
                this.data2 = single.base == single.fill ? "" : ExtendedStructureHandling.stringifyBlockState(single.base);
                this.data3 = single.cap == single.fill ? "" : ExtendedStructureHandling.stringifyBlockState(single.cap);
                break;
            }
            case LAYER:
                this.data1 = be.getData().asLayer().structure.toString();
                this.data2 = this.data3 = "";
                break;
            case JIGSAW:
                this.data1 = be.getData().asJigsaw().startPool.toString();
                this.data2 = this.data3 = "";
                break;
        }
    }

    public void writeToBlockEntity(StructureRepeaterBlockEntity be) {
        be.setMinRepeat(this.minRepeat);
        be.setMaxRepeat(this.maxRepeat);
        be.setStopAtSolid(this.stopAtSolid);
        be.setReplacementState(ExtendedStructureHandling.parseBlockState(this.replacement));
        StructureRepeaterBlockEntity.RepeaterData data;
        switch(this.mode) {
            case SINGLE: {
                BlockState fill = ExtendedStructureHandling.parseBlockState(this.data1);
                BlockState base = this.data2.isEmpty() ? fill : ExtendedStructureHandling.parseBlockState(this.data2);
                BlockState cap = this.data3.isEmpty() ? fill : ExtendedStructureHandling.parseBlockState(this.data3);
                data = new StructureRepeaterBlockEntity.Single(fill, base, cap);
                break;
            }
            case LAYER:
                data = new StructureRepeaterBlockEntity.Layer(parseOrDefault(this.data1));
                break;
            case JIGSAW:
                data = new StructureRepeaterBlockEntity.Jigsaw(parseOrDefault(this.data1));
                break;
            default:
                throw new AssertionError(this.mode);
        }
        be.setData(data);
    }

    private static Identifier parseOrDefault(String str) {
        Identifier id = Identifier.tryParse(str);
        return id == null ? new Identifier("minecraft:empty") : id;
    }
}
