package robosky.structurehelpers.block;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.structure.ExtendedStructureHandling;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
public class StructureRepeaterBlockEntity extends BlockEntity {

    public static final RepeaterData DEFAULT_DATA = new Single(Blocks.AIR.getDefaultState(),
        Blocks.AIR.getDefaultState(),
        Blocks.AIR.getDefaultState());

    private RepeaterData data = DEFAULT_DATA;
    private int minRepeat = 1;
    private int maxRepeat = 1;
    private boolean stopAtSolid = false;
    private BlockState replacementState = Blocks.AIR.getDefaultState();

    public StructureRepeaterBlockEntity(BlockPos pos, BlockState state) {
        super(StructureHelpers.STRUCTURE_REPEATER_ENTITY_TYPE, pos, state);
    }

    public RepeaterData getData() {
        return data;
    }

    public void setData(RepeaterData data) {
        this.data = data;
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

    public BlockState getReplacementState() {
        return replacementState;
    }

    public void setReplacementState(BlockState replacementState) {
        this.replacementState = replacementState;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        NbtCompound data = tag.getCompound("Data");
        this.data = RepeaterData.CODEC
            .parse(NbtOps.INSTANCE, data)
            .result()
            .orElse(DEFAULT_DATA);
        this.minRepeat = Math.max(0, tag.getInt("RepeatMin"));
        this.maxRepeat = Math.max(this.minRepeat, tag.getInt("RepeatMax"));
        this.stopAtSolid = tag.getBoolean("StopAtSolid");
        this.replacementState = ExtendedStructureHandling.parseBlockState(tag.getString("Replacement"));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag = super.writeNbt(tag);
        NbtCompound data = (NbtCompound)RepeaterData.CODEC
            .encodeStart(NbtOps.INSTANCE, this.data)
            .result()
            .orElseGet(NbtCompound::new);
        tag.put("Data", data);
        tag.putInt("RepeatMin", this.minRepeat);
        tag.putInt("RepeatMax", this.maxRepeat);
        tag.putBoolean("StopAtSolid", this.stopAtSolid);
        tag.putString("Replacement", ExtendedStructureHandling.stringifyBlockState(this.replacementState));
        return tag;
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
            Codec.STRING
                .fieldOf("BlockState")
                .xmap(ExtendedStructureHandling::parseBlockState, ExtendedStructureHandling::stringifyBlockState)
                .orElse(Blocks.AIR.getDefaultState())
                .forGetter(d -> d.fill),
            Codec.STRING
                .xmap(ExtendedStructureHandling::parseBlockState, ExtendedStructureHandling::stringifyBlockState)
                .orElse(Blocks.AIR.getDefaultState())
                .optionalFieldOf("BaseState")
                .forGetter(d -> Optional.of(d.base).filter(b -> b != d.fill)),
            Codec.STRING
                .xmap(ExtendedStructureHandling::parseBlockState, ExtendedStructureHandling::stringifyBlockState)
                .orElse(Blocks.AIR.getDefaultState())
                .optionalFieldOf("CapState")
                .forGetter(d -> Optional.of(d.cap).filter(c -> c != d.fill))
        ).apply(inst, (f, b, c) -> new Single(f, b.orElse(f), c.orElse(f))));

        public final BlockState fill;
        public final BlockState base;
        public final BlockState cap;

        public Single(BlockState fill, BlockState base, BlockState cap) {
            super(Mode.SINGLE);
            this.fill = fill;
            this.base = base;
            this.cap = cap;
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

        public Layer(Identifier structure) {
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

        public Jigsaw(Identifier startPool) {
            super(Mode.JIGSAW);
            this.startPool = startPool;
        }
    }

    /**
     * Base class for repeater data.
     */
    public static abstract class RepeaterData {

        public static final Codec<RepeaterData> CODEC = Mode.CODEC.dispatch(
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

        public final Mode mode;

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
