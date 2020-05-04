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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import robosky.structurehelpers.iface.JigsawAccessorData;
import robosky.structurehelpers.network.ServerStructHelpPackets;

import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.JigsawBlockScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;

@Mixin(JigsawBlockScreen.class)
public abstract class JigsawBlockScreenMixin extends Screen {

    @Unique
    private ButtonWidget junctionTypeButton;

    @Unique
    private boolean childJunction;

    @Shadow @Final private JigsawBlockEntity jigsaw;

    private JigsawBlockScreenMixin() {
        super(null);
    }

    @Inject(method = "updateServer", at = @At("RETURN"))
    private void sendCxnTypeToServer(CallbackInfo info) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(jigsaw.getPos());
        buf.writeBoolean(childJunction);
        ClientSidePacketRegistry.INSTANCE.sendToServer(ServerStructHelpPackets.JIGSAW_OFFSET_UPDATE, buf);
    }

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            ordinal = 3,
            shift = At.Shift.AFTER,
            remap = false
        )
    )
    private void initCxnTypeField(CallbackInfo info) {
        junctionTypeButton = this.addButton(new ButtonWidget(
            this.width / 2 - 154, 180, 150, 20,
            new LiteralText("<uninitialized>"),
            btn -> {
                childJunction ^= true;
                updateJunctionTypeButton();
            }
        ));
        // this.children.add(junctionTypeButton);
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

    @ModifyArg(
        method = "init",
        index = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/ButtonWidget;<init>(IIIILjava/lang/String;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)V",
            ordinal = 2
        )
    )
    private int relocateJointButtonVertically(int posY) {
        return 180;
    }

    @ModifyArg(
        method = "init",
        index = 2,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/ButtonWidget;<init>(IIIILjava/lang/String;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)V",
            ordinal = 2
        )
    )
    private int adjustJointButtonLength(int length) {
        return 150;
    }

    @Unique
    private void updateJunctionTypeButton() {
        junctionTypeButton.setMessage(new TranslatableText("jigsaw_block.structurehelpers.connection_type."
            + (childJunction ? "child" : "normal")));
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(IIF)V",
            ordinal = 3,
            shift = At.Shift.AFTER
        )
    )
    private void renderJunctionTypeField(MatrixStack matrix, int x, int y, float f, CallbackInfo info) {
        this.drawString(matrix, this.textRenderer, I18n.translate("jigsaw_block.structurehelpers.connection_type"), this.width / 2 - 153, 170, 10526880);
        junctionTypeButton.render(matrix, x, y, f);
    }

    @ModifyArg(
        method = "render",
        index = 2,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/JigsawBlockScreen;drawString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
            ordinal = 4
        )
    )
    private int offsetJointButtonLabelHorizontally(int posX) {
        return posX + 154 + 4;
    }

    @ModifyArg(
        method = "render",
        index = 3,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/JigsawBlockScreen;drawString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
            ordinal = 4
        )
    )
    private int offsetJointButtonLabelVertically(int posY) {
        return 170;
    }

    @ModifyArg(
        method = "render",
        index = 4,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/JigsawBlockScreen;drawString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
            ordinal = 4
        )
    )
    private int setJointButtonLabelColor(int color) {
        // gray
        return 10526880;
    }
}
