package robosky.structurehelpers.client;

import io.netty.buffer.Unpooled;
import robosky.structurehelpers.block.StructureRepeaterBlockEntity;
import robosky.structurehelpers.network.RepeaterPacketData;
import robosky.structurehelpers.network.ServerStructHelpPackets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class StructureRepeaterScreen extends Screen {

    private static final int GUI_RADIUS = 150;
    private static final int WIDGET_HEIGHT = 20;
    private static final int PADDING_H = 5;
    private static final int PADDING_V = 5;

    private final StructureRepeaterBlockEntity backingBe;

    // input fields
    private CyclingButtonWidget<StructureRepeaterBlockEntity.Mode> modeIn;
    private TextFieldWidget minRepeatIn;
    private TextFieldWidget maxRepeatIn;
    private CyclingButtonWidget<Boolean> stopAtSolidBtn;
    private TextFieldWidget replacement;

    // mode-specific fields
    private TextFieldWidget modeSpecificIn;
    private String singleText = "minecraft:air";
    private String layerText = "minecraft:empty";
    private String jigsawText = "minecraft:empty";

    // done button
    private ButtonWidget doneBtn;

    // UI positioning
    private int centerH;
    private int centerV;

    public StructureRepeaterScreen(StructureRepeaterBlockEntity backingBe) {
        super(NarratorManager.EMPTY);
        this.backingBe = backingBe;
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
        this.minRepeatIn = this.addChild(new TextFieldWidget(
            this.textRenderer,
            this.centerH - GUI_RADIUS + 1,
            this.centerV - 3 * (WIDGET_HEIGHT + PADDING_V),
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.min_repeat")));
        this.minRepeatIn.setMaxLength(9);
        this.minRepeatIn.setText(Integer.toString(this.backingBe.getMinRepeat()));
        this.minRepeatIn.setTextPredicate(t -> t.matches("\\d*"));
        this.minRepeatIn.setChangedListener(text -> this.updateDoneButton());
        this.maxRepeatIn = this.addChild(new TextFieldWidget(
            this.textRenderer,
            this.centerH + PADDING_H,
            this.centerV - 3 * (WIDGET_HEIGHT + PADDING_V),
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.max_repeat")));
        this.maxRepeatIn.setMaxLength(9);
        this.maxRepeatIn.setText(Integer.toString(this.backingBe.getMaxRepeat()));
        this.maxRepeatIn.setTextPredicate(t -> t.matches("\\d*"));
        this.maxRepeatIn.setChangedListener(text -> this.updateDoneButton());
        this.stopAtSolidBtn = this.addButton(CyclingButtonWidget
            .<Boolean>builder(b -> new TranslatableText("gui.structure-helpers.repeater.termination." + (b ? "surface" : "random")))
            .values(false, true)
            .initially(this.backingBe.stopsAtSolid())
            .build(this.centerH - GUI_RADIUS,
                this.centerV - (3 * (WIDGET_HEIGHT + PADDING_V) / 2),
                GUI_RADIUS - PADDING_H,
                WIDGET_HEIGHT,
                new TranslatableText("gui.structure-helpers.repeater.termination"),
                (btn, v) -> {
                }));
        this.replacement = this.addChild(new TextFieldWidget(
            this.textRenderer,
            this.centerH + PADDING_H,
            this.centerV - (3 * (WIDGET_HEIGHT + PADDING_V) / 2),
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.replacement")));
        this.replacement.setMaxLength(4096);
        this.replacement.setText(this.backingBe.getReplacementState());
        this.modeIn = this.addButton(CyclingButtonWidget
            .<StructureRepeaterBlockEntity.Mode>builder(m -> new TranslatableText("gui.structure-helpers.repeater.mode." + m.asString()))
            .values(StructureRepeaterBlockEntity.Mode.values())
            .initially(this.backingBe.getMode())
            .build(this.centerH - GUI_RADIUS,
                this.centerV,
                GUI_RADIUS - PADDING_H,
                WIDGET_HEIGHT,
                new TranslatableText("gui.structure-helpers.repeater.mode"),
                (btn, v) -> this.updateModeSpecificState(v)));
        this.modeSpecificIn = this.addChild(new TextFieldWidget(
            this.textRenderer,
            this.centerH + PADDING_H,
            this.centerV,
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater")));
        this.modeSpecificIn.setMaxLength(4096);
        this.modeSpecificIn.setTextPredicate(this::modeSpecificTextPredicate);
        this.modeSpecificIn.setChangedListener(this::saveModeSpecificState);
        this.setInitialModeSpecificState();
        this.doneBtn = this.addButton(new ButtonWidget(this.centerH - GUI_RADIUS,
            this.centerV + 2 * (WIDGET_HEIGHT + PADDING_V),
            GUI_RADIUS - PADDING_H,
            WIDGET_HEIGHT,
            ScreenTexts.DONE,
            btn -> this.sendDataToServer()));
        this.addButton(new ButtonWidget(this.centerH + PADDING_H,
            this.centerV + 2 * (WIDGET_HEIGHT + PADDING_V),
            GUI_RADIUS - PADDING_H,
            WIDGET_HEIGHT,
            ScreenTexts.CANCEL,
            btn -> this.onClose()));
    }

    private void updateDoneButton() {
        try {
            int min = Integer.parseInt(this.minRepeatIn.getText());
            int max = Integer.parseInt(this.maxRepeatIn.getText());
            this.doneBtn.active = min <= max;
        } catch(NumberFormatException e) {
            this.doneBtn.active = false;
        }
    }

    private void setInitialModeSpecificState() {
        StructureRepeaterBlockEntity.RepeaterData data = this.backingBe.getData();
        StructureRepeaterBlockEntity.Mode mode = this.backingBe.getMode();
        switch(mode) {
            case SINGLE:
                this.singleText = data.asSingle().serializedState;
                break;
            case LAYER:
                this.layerText = data.asLayer().structure.toString();
                break;
            case JIGSAW:
                this.jigsawText = data.asJigsaw().startPool.toString();
                break;
        }
        this.updateModeSpecificState(mode);
    }

    private void updateModeSpecificState(StructureRepeaterBlockEntity.Mode mode) {
        switch(mode) {
            case SINGLE:
                this.modeSpecificIn.setText(this.singleText);
                break;
            case LAYER:
                this.modeSpecificIn.setText(this.layerText);
                break;
            case JIGSAW:
                this.modeSpecificIn.setText(this.jigsawText);
                break;
        }
    }

    private void saveModeSpecificState(String modeSpecific) {
        switch(this.modeIn.getValue()) {
            case SINGLE:
                this.singleText = modeSpecific;
                break;
            case LAYER:
                this.layerText = modeSpecific;
                break;
            case JIGSAW:
                this.jigsawText = modeSpecific;
                break;
        }
    }

    private boolean modeSpecificTextPredicate(String text) {
        switch(this.modeIn.getValue()) {
            case LAYER:
            case JIGSAW:
                return Identifier.isValid(text);
            default:
                return true;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        // title
        DrawableHelper.drawCenteredString(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.repeater.title"),
            this.centerH,
            this.centerV - (11 * WIDGET_HEIGHT / 2),
            0xffffff);
        // min repeat
        DrawableHelper.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.repeater.min_repeat"),
            this.minRepeatIn.x,
            this.minRepeatIn.y - WIDGET_HEIGHT / 2,
            0xa0a0a0);
        this.minRepeatIn.render(matrices, mouseX, mouseY, delta);
        // max repeat
        DrawableHelper.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.repeater.max_repeat"),
            this.maxRepeatIn.x,
            this.maxRepeatIn.y - WIDGET_HEIGHT / 2,
            0xa0a0a0);
        this.maxRepeatIn.render(matrices, mouseX, mouseY, delta);
        // replacement state
        DrawableHelper.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.repeater.replacement"),
            this.replacement.x,
            this.replacement.y - WIDGET_HEIGHT / 2,
            0xa0a0a0);
        this.replacement.render(matrices, mouseX, mouseY, delta);
        // mode specific
        String modeSpecificSetting;
        switch(this.modeIn.getValue()) {
            case SINGLE:
                modeSpecificSetting = "single.state";
                break;
            case LAYER:
                modeSpecificSetting = "layer.structure";
                break;
            case JIGSAW:
                modeSpecificSetting = "jigsaw.start";
                break;
            default:
                throw new AssertionError("unknown mode");
        }
        DrawableHelper.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.repeater.mode." + modeSpecificSetting),
            this.modeSpecificIn.x,
            this.modeSpecificIn.y - WIDGET_HEIGHT / 2,
            0xa0a0a0);
        this.modeSpecificIn.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void sendDataToServer() {
        RepeaterPacketData data =
            new RepeaterPacketData(backingBe.getPos(),
                Integer.parseInt(this.minRepeatIn.getText()),
                Integer.parseInt(this.maxRepeatIn.getText()),
                this.stopAtSolidBtn.getValue(),
                this.replacement.getText(),
                this.modeIn.getValue(),
                this.modeSpecificIn.getText());
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        data.write(buf);
        ClientPlayNetworking.send(ServerStructHelpPackets.REPEATER_UPDATE, buf);
        this.onClose();
    }
}
