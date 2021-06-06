package robosky.structurehelpers.mixin.client;

import io.netty.buffer.Unpooled;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import robosky.structurehelpers.iface.JigsawAccessorData;
import robosky.structurehelpers.network.ServerStructHelpPackets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.JigsawBlockScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Direction;

@Mixin(JigsawBlockScreen.class)
abstract class JigsawBlockScreenMixin extends Screen {

    @Unique
    private ButtonWidget junctionTypeButton;

    @Unique
    private boolean childJunction;

    @Shadow
    @Final
    private JigsawBlockEntity jigsaw;

    private JigsawBlockScreenMixin() {
        super(null);
    }

    @Inject(method = "updateServer", at = @At("RETURN"))
    private void sendCxnTypeToServer(CallbackInfo info) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(jigsaw.getPos());
        buf.writeBoolean(childJunction);
        ClientPlayNetworking.send(ServerStructHelpPackets.JIGSAW_OFFSET_UPDATE, buf);
    }

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/JigsawBlockScreen;addSelectableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;",
            ordinal = 3,
            shift = At.Shift.AFTER
        )
    )
    private void initCxnTypeField(CallbackInfo info) {
        junctionTypeButton = this.addDrawableChild(new ButtonWidget(
            this.width / 2 - 152, 150, 150, 20,
            new LiteralText("<uninitialized>"),
            btn -> {
                childJunction ^= true;
                updateJunctionTypeButton();
            }
        ));
        childJunction = ((JigsawAccessorData)this.jigsaw).structhelp_isChildJunction();
        updateJunctionTypeButton();
    }

    @ModifyVariable(
        method = "init",
        ordinal = 0,
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private int relocateJointButtonHorizontally(int offsetX) {
        // cancels (this.width / 2 - 152)
        return 152 + 4;
    }

    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/CyclingButtonWidget$Builder;omitKeyText()Lnet/minecraft/client/gui/widget/CyclingButtonWidget$Builder;"
        )
    )
    private CyclingButtonWidget.Builder<?> doNotOmitKeyTextForJointBtn(CyclingButtonWidget.Builder<?> self) {
        return self;
    }

    @ModifyArg(
        method = "init",
        index = 2,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/CyclingButtonWidget$Builder;build(IIIILnet/minecraft/text/Text;Lnet/minecraft/client/gui/widget/CyclingButtonWidget$UpdateCallback;)Lnet/minecraft/client/gui/widget/CyclingButtonWidget;",
            ordinal = 0
        )
    )
    private int adjustJointButtonLength(int length) {
        return 150;
    }

    @Unique
    private void updateJunctionTypeButton() {
        junctionTypeButton.setMessage(
            new TranslatableText("jigsaw_block.structurehelpers.connection_type")
                .append(" ")
                .append(new TranslatableText(
                    "jigsaw_block.structurehelpers.connection_type." + (childJunction ? "child" : "normal")
                ))
        );
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
            ordinal = 3,
            shift = At.Shift.AFTER
        )
    )
    private void renderJunctionTypeField(MatrixStack matrix, int x, int y, float f, CallbackInfo info) {
        junctionTypeButton.render(matrix, x, y, f);
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Direction$Axis;isVertical()Z"
        )
    )
    private boolean cancelJointButtonLabelRendering(Direction.Axis axis) {
        return false;
    }
}
