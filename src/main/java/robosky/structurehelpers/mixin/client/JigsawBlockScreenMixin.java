package robosky.structurehelpers.mixin.client;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;

import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.JigsawBlockScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.PacketByteBuf;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import robosky.structurehelpers.iface.JigsawAccessorData;
import robosky.structurehelpers.network.ServerStructHelpPackets;

@Mixin(JigsawBlockScreen.class)
public abstract class JigsawBlockScreenMixin extends Screen {

    @Unique
    private ButtonWidget junctionTypeButton;

    @Unique
    private boolean childJunction;

    @Shadow @Final private JigsawBlockEntity jigsaw;
    @Shadow private ButtonWidget doneButton;

    @Shadow protected native void updateDoneButtonState();

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
            ordinal = 2,
            shift = At.Shift.AFTER,
            remap = false
        )
    )
    private void initCxnTypeField(CallbackInfo info) {
        junctionTypeButton = this.addButton(new ButtonWidget(
            this.width / 2 - 154, 160, 150, 20,
            "<uninitialized>",
            btn -> {
                childJunction ^= true;
                updateJunctionTypeButton();
            }
        ));
        this.children.add(junctionTypeButton);
        childJunction = ((JigsawAccessorData)this.jigsaw).structhelp_isChildJunction();
        updateJunctionTypeButton();
    }

    @Unique
    private void updateJunctionTypeButton() {
        junctionTypeButton.setMessage(I18n.translate("jigsaw_block.structurehelpers.connection_type."
            + (childJunction ? "child" : "normal")));
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
        this.drawString(this.font, I18n.translate("jigsaw_block.structurehelpers.connection_type"), this.width / 2 - 153, 150, 10526880);
        junctionTypeButton.render(x, y, f);
    }
}
