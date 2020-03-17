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

import robosky.structurehelpers.iface.JigsawAccessorData;

@Mixin(JigsawBlockEntity.class)
public abstract class JigsawBlockEntityMixin extends BlockEntity implements JigsawAccessorData {

    /**
     * Whether this jigsaw is a normal junction or a child junction. Child
     * junctions are not checked for collision and may be placed anywhere
     * within a structure pool element. This allows for e.g. flush doorways
     * like those that generate in vanilla strongholds.
     */
    @Unique
    private boolean childJunction;

    private JigsawBlockEntityMixin() {
        super(null);
    }

    @Override
    public boolean structhelp_isChildJunction() {
        return childJunction;
    }

    @Override
    public void structhelp_setChildJunction(boolean child) {
        this.childJunction = child;
    }

    @Inject(method = "toTag", at = @At("RETURN"))
    private void writeOffsetToTag(CompoundTag tag, CallbackInfoReturnable<CompoundTag> info) {
        tag.putBoolean(CHILD_JUNCTION, childJunction);
    }

    @Inject(method = "fromTag", at = @At("RETURN"))
    private void readOffsetFromTag(CompoundTag tag, CallbackInfo info) {
        childJunction = tag.getBoolean(CHILD_JUNCTION);
    }
}
