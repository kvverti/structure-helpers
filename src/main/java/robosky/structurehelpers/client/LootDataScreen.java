package robosky.structurehelpers.client;

import io.netty.buffer.Unpooled;
import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.network.LootDataPacketData;
import robosky.structurehelpers.network.ServerStructHelpPackets;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;

/**
 * Screen class for the loot data block entity.
 */
public class LootDataScreen extends Screen {

    private static final int guiRadius = 150;

    // relevant input fields
    private final LootDataBlockEntity backingBe;
    private TextFieldWidget lootTableIn;
    private TextFieldWidget replaceIn;
    private ButtonWidget doneBtn;

    // center screen position, for convenience
    private int centerH;
    private int centerV;

    public LootDataScreen(LootDataBlockEntity be) {
        super(NarratorManager.EMPTY);
        this.backingBe = be;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        centerH = this.width / 2;
        centerV = this.height / 2;
        assert this.client != null : "this.client null in LootDataScreen";
        this.client.keyboard.setRepeatEvents(true);
        // loot table ID
        lootTableIn = new TextFieldWidget(this.textRenderer,
            centerH - guiRadius,
            centerV - 40,
            guiRadius * 2,
            20,
            new TranslatableText("gui.structure-helpers.loot_data.table"));
        lootTableIn.setMaxLength(4096);
        lootTableIn.setText(backingBe.getLootTable().toString());
        lootTableIn.setChangedListener(text -> updateDoneButton());
        this.children.add(lootTableIn);
        // replacement state
        replaceIn = new TextFieldWidget(this.textRenderer,
            centerH - guiRadius,
            centerV,
            guiRadius * 2,
            20,
            new TranslatableText("gui.structure-helpers.loot_data.replace"));
        replaceIn.setMaxLength(4096);
        replaceIn.setText(backingBe.getReplacementState());
        replaceIn.setChangedListener(text -> updateDoneButton());
        this.children.add(replaceIn);
        // done + cancel buttons
        final int padding = 5;
        final int btnWidth = guiRadius - padding + 1;
        doneBtn = this.addButton(new ButtonWidget(centerH - padding - btnWidth,
            centerV + 40,
            btnWidth,
            20,
            ScreenTexts.DONE,
            btn -> this.sendDataToServer()));
        this.addButton(new ButtonWidget(centerH + padding,
            centerV + 40,
            btnWidth,
            20,
            ScreenTexts.CANCEL,
            btn -> this.onClose()));
    }

    private void updateDoneButton() {
        doneBtn.active = Identifier.isValid(lootTableIn.getText());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        // title
        DrawableHelper.drawCenteredString(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.loot_data.title"),
            centerH,
            centerV - 80,
            0xffffff);
        // loot table ID
        DrawableHelper.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.loot_data.table"),
            centerH - guiRadius,
            lootTableIn.y - 10,
            0xa0a0a0);
        lootTableIn.render(matrices, mouseX, mouseY, delta);
        // replacement state
        DrawableHelper.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.loot_data.replace"),
            centerH - guiRadius,
            replaceIn.y - 10,
            0xa0a0a0);
        replaceIn.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void sendDataToServer() {
        LootDataPacketData data =
            new LootDataPacketData(backingBe.getPos(), lootTableIn.getText(), replaceIn.getText());
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        data.write(buf);
        ClientSidePacketRegistry.INSTANCE.sendToServer(ServerStructHelpPackets.LOOT_DATA_UPDATE, buf);
        this.onClose();
    }
}
