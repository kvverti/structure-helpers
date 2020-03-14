package robosky.structurehelpers.mixin.client;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;

import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.JigsawBlockScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.PacketByteBuf;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import robosky.structurehelpers.iface.JigsawOffsetData;
import robosky.structurehelpers.network.ServerStructHelpPackets;

@Mixin(JigsawBlockScreen.class)
public abstract class JigsawBlockScreenMixin extends Screen {

    @Unique
    private TextFieldWidget offsetField;

    @Shadow @Final private JigsawBlockEntity jigsaw;
    @Shadow private ButtonWidget doneButton;

    @Shadow protected native void updateDoneButtonState();

    private JigsawBlockScreenMixin() {
        super(null);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickOffsetField(CallbackInfo info) {
        offsetField.tick();
    }

    @Inject(method = "updateServer", at = @At("RETURN"))
    private void sendOffsetToServer(CallbackInfo info) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(jigsaw.getPos());
        buf.writeByte(Integer.parseInt(offsetField.getText()));
        ClientSidePacketRegistry.INSTANCE.sendToServer(ServerStructHelpPackets.JIGSAW_OFFSET_UPDATE, buf);
    }

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            ordinal = 1,
            shift = At.Shift.AFTER,
            remap = false
        )
    )
    private void initOffsetField(CallbackInfo info) {
        offsetField = new TextFieldWidget(
            this.textRenderer,
            this.width / 2 - 152,
            120,
            300,
            20,
            I18n.translate("jigsaw_block.structurehelpers.offset"));
        offsetField.setMaxLength(3);
        offsetField.setText(Integer.toString(((JigsawOffsetData)this.jigsaw).structhelp_getOffset()));
        offsetField.setChangedListener(s -> this.updateDoneButtonState());
        this.children.add(offsetField);
    }

    @ModifyArg(
        method = "init",
        index = 2,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;<init>(Lnet/minecraft/client/font/TextRenderer;IIIILjava/lang/String;)V",
            ordinal = 2
        )
    )
    private int initFinalStateFieldToForthRow(int i) {
        return 160;
    }

    @Inject(method = "updateDoneButtonState", at = @At("RETURN"))
    private void updateDoneButtonFromOffset(CallbackInfo info) {
        this.doneButton.active &= offsetField.getText().matches("\\d+");
    }

    @Unique
    private String offsetText;

    @Inject(method = "resize", at = @At("HEAD"))
    private void saveOffsetOnResize(CallbackInfo info) {
        offsetText = offsetField.getText();
    }

    @Inject(method = "resize", at = @At("RETURN"))
    private void restoreOffsetOnResize(CallbackInfo info) {
        offsetField.setText(offsetText);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(IIF)V",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    private void renderOffsetField(int x, int y, float f, CallbackInfo info) {
        this.drawString(this.textRenderer, I18n.translate("jigsaw_block.structurehelpers.offset"), this.width / 2 - 153, 110, 10526880);
        offsetField.render(x, y, f);
    }

    @ModifyArg(
        method = "render",
        index = 3,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/JigsawBlockScreen;drawString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
            ordinal = 2
        )
    )
    private int renderFinalStateFieldToForthRow(int i) {
        return 150;
    }
}
