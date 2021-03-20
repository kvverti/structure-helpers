package robosky.structurehelpers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import robosky.structurehelpers.StructureHelpers;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

/**
 * Structure repeaters store data that can generate repeating columns of blocks. Useful for nether fortresses,
 * mineshafts, etc.
 * <p>
 * A structure repeater may have one of these modes:
 * <ul>
 *   <li>single column. Defines a single block state to repeat</li>
 *   <li>layer. Defines a structure to stack</li>
 *   <li>jigsaw. Strings structure pool elements using rules for jigsaw connection.
 *       This mode only expends a single normal junction in the direction of repetition.</li>
 * </ul>
 */
public class StructureRepeaterBlockEntity extends BlockEntity implements BlockEntityClientSerializable {

    private static final RepeaterData DEFAULT = new Single("minecraft:air");

    private RepeaterData data = DEFAULT;
    private int minRepeat = 1;
    private int maxRepeat = 1;
    private boolean stopAtSolid = false;

    public StructureRepeaterBlockEntity(BlockPos pos, BlockState state) {
        super(StructureHelpers.STRUCTURE_REPEATER_ENTITY_TYPE, pos, state);
    }

    public RepeaterData getData() {
        return data;
    }

    public Mode getMode() {
        return this.data.mode;
    }

    public int getMinRepeat() {
        return minRepeat;
    }

    public void setMinRepeat(int minRepeat) {
        this.minRepeat = minRepeat;
    }

    public int getMaxRepeat() {
        return maxRepeat;
    }

    public void setMaxRepeat(int maxRepeat) {
        this.maxRepeat = maxRepeat;
    }

    public boolean stopsAtSolid() {
        return stopAtSolid;
    }

    public void setStopAtSolid(boolean stopAtSolid) {
        this.stopAtSolid = stopAtSolid;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.fromClientTag(tag);
    }

    @Override
    public void fromClientTag(NbtCompound nbt) {
        NbtCompound data = nbt.getCompound("Data");
        this.data = RepeaterData.CODEC
            .parse(NbtOps.INSTANCE, data)
            .result()
            .orElse(DEFAULT);
        this.minRepeat = Math.max(0, nbt.getInt("RepeatMin"));
        this.maxRepeat = Math.max(this.minRepeat, nbt.getInt("RepeatMax"));
        this.stopAtSolid = nbt.getBoolean("StopAtSolid");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag = super.writeNbt(tag);
        return this.toClientTag(tag);
    }

    @Override
    public NbtCompound toClientTag(NbtCompound nbt) {
        NbtCompound data = (NbtCompound)RepeaterData.CODEC
            .encodeStart(NbtOps.INSTANCE, this.data)
            .result()
            .orElseGet(NbtCompound::new);
        nbt.put("Data", data);
        nbt.putInt("RepeatMin", this.minRepeat);
        nbt.putInt("RepeatMax", this.maxRepeat);
        nbt.putBoolean("StopAtSolid", this.stopAtSolid);
        return nbt;
    }

    /**
     * Type discriminant for repeater data.
     */
    public enum Mode implements StringIdentifiable {
        SINGLE("single"),
        LAYER("layer"),
        JIGSAW("jigsaw");

        static final Codec<Mode> CODEC = StringIdentifiable.createCodec(Mode::values, s -> {
            switch(s) {
                case "layer":
                    return LAYER;
                case "jigsaw":
                    return JIGSAW;
                default:
                    return SINGLE;
            }
        });

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }

    /**
     * Repeat a single block state.
     */
    public static final class Single extends RepeaterData {

        static final Codec<Single> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("BlockState").orElse("minecraft:air").forGetter(d -> d.serializedState)
        ).apply(inst, Single::new));

        public final String serializedState;

        Single(String state) {
            super(Mode.SINGLE);
            this.serializedState = state;
        }
    }

    /**
     * Repeat a structure.
     */
    public static final class Layer extends RepeaterData {

        static final Codec<Layer> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("Structure").orElseGet(() -> new Identifier("empty")).forGetter(d -> d.structure)
        ).apply(inst, Layer::new));

        public final Identifier structure;

        Layer(Identifier structure) {
            super(Mode.LAYER);
            this.structure = structure;
        }
    }

    /**
     * Start a jigsaw iteration.
     */
    public static final class Jigsaw extends RepeaterData {

        static final Codec<Jigsaw> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("Start").orElseGet(() -> new Identifier("empty")).forGetter(d -> d.startPool)
        ).apply(inst, Jigsaw::new));

        public final Identifier startPool;

        Jigsaw(Identifier startPool) {
            super(Mode.JIGSAW);
            this.startPool = startPool;
        }
    }

    /**
     * Base class for repeater data.
     */
    public static abstract class RepeaterData {

        static final Codec<RepeaterData> CODEC = Mode.CODEC.dispatch(
            "Mode",
            d -> d.mode,
            mode -> {
                switch(mode) {
                    case LAYER:
                        return Layer.CODEC;
                    case JIGSAW:
                        return Jigsaw.CODEC;
                    default:
                        return Single.CODEC;
                }
            }
        );

        final Mode mode;

        protected RepeaterData(Mode mode) {
            this.mode = mode;
        }

        public Single asSingle() {
            return (Single)this;
        }

        public Layer asLayer() {
            return (Layer)this;
        }

        public Jigsaw asJigsaw() {
            return (Jigsaw)this;
        }
    }
}
