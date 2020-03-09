package robosky.structurehelpers.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.nbt.CompoundTag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import robosky.structurehelpers.iface.JigsawOffsetData;

@Mixin(JigsawBlockEntity.class)
public abstract class JigsawBlockEntityMixin extends BlockEntity implements JigsawOffsetData {

    /**
     * The offset is how many blocks in or out this jigsaw junction
     * should be translated. This allows for e.g. flush doorways like
     * those that generate in vanilla strongholds.
     */
    @Unique
    private int offset;

    private JigsawBlockEntityMixin() {
        super(null);
    }

    @Override
    public int structhelp_getOffset() {
        return offset;
    }

    @Override
    public void structhelp_setOffset(int offset) {
        this.offset = offset & 0xff;
    }

    @Inject(method = "toTag", at = @At("RETURN"))
    private void writeOffsetToTag(CompoundTag tag, CallbackInfoReturnable<CompoundTag> info) {
        tag.putByte("StructHelp_Offset", (byte)offset);
    }

    @Inject(method = "fromTag", at = @At("RETURN"))
    private void readOffsetFromTag(CompoundTag tag, CallbackInfo info) {
        offset = tag.getByte("StructHelp_Offset");
    }
}
