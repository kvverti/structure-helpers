package robosky.structurehelpers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import robosky.structurehelpers.StructureHelpers;

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
public class StructureRepeaterBlockEntity extends BlockEntity {

    private static final RepeaterData DEFAULT = new Single("minecraft:air");

    private RepeaterData data = DEFAULT;
    private int repeatMin = 1;
    private int repeatMax = 1;
    private boolean stopAtSolid = false;

    public StructureRepeaterBlockEntity(BlockPos pos, BlockState state) {
        super(StructureHelpers.STRUCTURE_REPEATER_ENTITY_TYPE, pos, state);
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        NbtCompound data = tag.getCompound("Data");
        this.data = RepeaterData.CODEC
            .parse(NbtOps.INSTANCE, data)
            .result()
            .orElse(DEFAULT);
        this.repeatMin = Math.max(0, tag.getInt("RepeatMin"));
        this.repeatMax = Math.max(this.repeatMin, tag.getInt("RepeatMax"));
        this.stopAtSolid = tag.getBoolean("StopAtSolid");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag = super.writeNbt(tag);
        NbtCompound data = (NbtCompound)RepeaterData.CODEC
            .encodeStart(NbtOps.INSTANCE, this.data)
            .result()
            .orElseGet(NbtCompound::new);
        tag.put("Data", data);
        tag.putInt("RepeatMin", this.repeatMin);
        tag.putInt("RepeatMax", this.repeatMax);
        tag.putBoolean("StopAtSolid", this.stopAtSolid);
        return tag;
    }

    /**
     * Type discriminant for repeater data.
     */
    private enum Mode implements StringIdentifiable {
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
    private static final class Single extends RepeaterData {

        static final Codec<Single> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("BlockState").orElse("minecraft:air").forGetter(d -> d.serializedState)
        ).apply(inst, Single::new));

        final String serializedState;

        Single(String state) {
            super(Mode.SINGLE);
            this.serializedState = state;
        }
    }

    /**
     * Repeat a structure.
     */
    private static final class Layer extends RepeaterData {

        static final Codec<Layer> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("Structure").orElseGet(() -> new Identifier("empty")).forGetter(d -> d.structure)
        ).apply(inst, Layer::new));

        final Identifier structure;

        Layer(Identifier structure) {
            super(Mode.LAYER);
            this.structure = structure;
        }
    }

    /**
     * Start a jigsaw iteration.
     */
    private static final class Jigsaw extends RepeaterData {

        static final Codec<Jigsaw> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("Start").orElseGet(() -> new Identifier("empty")).forGetter(d -> d.startPool)
        ).apply(inst, Jigsaw::new));

        final Identifier startPool;

        Jigsaw(Identifier startPool) {
            super(Mode.JIGSAW);
            this.startPool = startPool;
        }
    }

    /**
     * Base class for repeater data.
     */
    private static abstract class RepeaterData {

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
    }
}
