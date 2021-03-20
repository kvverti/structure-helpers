package robosky.structurehelpers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import robosky.structurehelpers.StructureHelpers;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
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
//    private int repeatMin;
//    private int repeatMax;
//    private boolean stopAtSolid;

    public StructureRepeaterBlockEntity(BlockPos pos, BlockState state) {
        super(StructureHelpers.STRUCTURE_REPEATER_ENTITY_TYPE, pos, state);
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.data = RepeaterData.CODEC.parse(NbtOps.INSTANCE, tag)
            .result()
            .orElse(DEFAULT);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag = super.writeNbt(tag);
        return (NbtCompound)RepeaterData.CODEC.encode(this.data, NbtOps.INSTANCE, tag).result().orElse(tag);
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
            Codec.STRING.fieldOf("BlockState").forGetter(d -> d.serializedState)
        ).apply(inst, Single::new));

        final String serializedState;

        Single(String state) {
            super(Mode.SINGLE);
            this.serializedState = state;
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
                    case JIGSAW:
                        throw new UnsupportedOperationException();
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
