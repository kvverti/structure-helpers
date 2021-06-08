package robosky.structurehelpers.client;

import java.util.List;

import io.netty.buffer.Unpooled;
import robosky.structurehelpers.block.StructureRepeaterBlockEntity;
import robosky.structurehelpers.network.RepeaterPacketData;
import robosky.structurehelpers.network.ServerStructHelpPackets;
import robosky.structurehelpers.structure.ExtendedStructureHandling;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
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
    private TextFieldWidget singleBaseIn;
    private TextFieldWidget singleFillIn;
    private TextFieldWidget singleCapIn;

    private TextFieldWidget jigsawStartIn;

    // index of the mode specific fields
    private int modeSpecificStart;

    // dummy input field to round out the child widgets
    private final TextFieldWidget dummy;

    // done button
    private ButtonWidget doneBtn;

    // UI positioning
    private int centerH;
    private int centerV;

    public StructureRepeaterScreen(StructureRepeaterBlockEntity backingBe) {
        super(NarratorManager.EMPTY);
        this.backingBe = backingBe;
        this.dummy = new TextFieldWidget(this.textRenderer, 0, 0, 0, 0, LiteralText.EMPTY);
        this.dummy.active = false;
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
        this.minRepeatIn = this.addSelectableChild(new TextFieldWidget(
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
        this.maxRepeatIn = this.addSelectableChild(new TextFieldWidget(
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
        this.stopAtSolidBtn = this.addDrawableChild(CyclingButtonWidget
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
        this.replacement = this.addSelectableChild(new TextFieldWidget(
            this.textRenderer,
            this.centerH + PADDING_H,
            this.centerV - (3 * (WIDGET_HEIGHT + PADDING_V) / 2),
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.replacement")));
        this.replacement.setMaxLength(4096);
        this.replacement.setText(ExtendedStructureHandling.stringifyBlockState(this.backingBe.getReplacementState()));
        this.replacement.setChangedListener(text -> this.updateDoneButton());
        // mode button
        this.modeIn = this.addDrawableChild(CyclingButtonWidget
            .<StructureRepeaterBlockEntity.Mode>builder(m -> new TranslatableText("gui.structure-helpers.repeater.mode." + m.asString()))
            .values(StructureRepeaterBlockEntity.Mode.values())
            .initially(this.backingBe.getMode())
            .build(this.centerH - GUI_RADIUS,
                this.centerV,
                GUI_RADIUS - PADDING_H,
                WIDGET_HEIGHT,
                new TranslatableText("gui.structure-helpers.repeater.mode"),
                (btn, v) -> this.changeMode(v)));
        // single mode
        this.singleFillIn = new TextFieldWidget(
            this.textRenderer,
            this.centerH + PADDING_H,
            this.centerV,
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.mode.single.base"));
        this.singleFillIn.setMaxLength(4096);
        this.singleFillIn.setText("minecraft:air");
        this.singleFillIn.setChangedListener(v -> this.updateDoneButton());
        this.singleBaseIn = new TextFieldWidget(
            this.textRenderer,
            this.centerH - GUI_RADIUS,
            this.centerV + (3 * (WIDGET_HEIGHT + PADDING_V) / 2),
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.mode.single.fill"));
        this.singleBaseIn.setMaxLength(4096);
        this.singleBaseIn.setChangedListener(v -> this.updateDoneButton());
        this.singleCapIn = new TextFieldWidget(
            this.textRenderer,
            this.centerH + PADDING_H,
            this.centerV + (3 * (WIDGET_HEIGHT + PADDING_V) / 2),
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.mode.single.cap"));
        this.singleCapIn.setMaxLength(4096);
        this.singleCapIn.setChangedListener(v -> this.updateDoneButton());
        // jigsaw mode
        this.jigsawStartIn = new TextFieldWidget(
            this.textRenderer,
            this.centerH + PADDING_H,
            this.centerV,
            GUI_RADIUS - PADDING_H - 2,
            WIDGET_HEIGHT,
            new TranslatableText("gui.structure-helpers.repeater.mode.jigsaw.start"));
        this.jigsawStartIn.setMaxLength(4096);
        this.jigsawStartIn.setTextPredicate(Identifier::isValid);
        this.jigsawStartIn.setText("minecraft:empty");
        this.jigsawStartIn.setChangedListener(v -> this.updateDoneButton());
        // fill the child slots with the dummy
        this.modeSpecificStart = this.children().size();
        this.addSelectableChild(this.dummy);
        this.addSelectableChild(this.dummy);
        this.addSelectableChild(this.dummy);
        // done and cancel
        this.doneBtn = this.addDrawableChild(new ButtonWidget(this.centerH - GUI_RADIUS,
            this.centerV + 3 * (WIDGET_HEIGHT + PADDING_V),
            GUI_RADIUS - PADDING_H,
            WIDGET_HEIGHT,
            ScreenTexts.DONE,
            btn -> this.sendDataToServer()));
        this.addDrawableChild(new ButtonWidget(this.centerH + PADDING_H,
            this.centerV + 3 * (WIDGET_HEIGHT + PADDING_V),
            GUI_RADIUS - PADDING_H,
            WIDGET_HEIGHT,
            ScreenTexts.CANCEL,
            btn -> this.onClose()));
        this.setInitialModeSpecificState();
    }

    private void updateDoneButton() {
        try {
            int min = Integer.parseInt(this.minRepeatIn.getText());
            int max = Integer.parseInt(this.maxRepeatIn.getText());
            this.doneBtn.active = min <= max &&
                ExtendedStructureHandling.isValidBlockState(this.replacement.getText());
            switch(this.modeIn.getValue()) {
                case SINGLE:
                    this.doneBtn.active &= ExtendedStructureHandling.isValidBlockState(this.singleFillIn.getText()) &&
                        (this.singleBaseIn.getText().isEmpty() || ExtendedStructureHandling.isValidBlockState(this.singleBaseIn.getText())) &&
                        (this.singleCapIn.getText().isEmpty() || ExtendedStructureHandling.isValidBlockState(this.singleCapIn.getText()));
                    break;
                case LAYER:
                    break;
                case JIGSAW:
                    this.doneBtn.active &= Identifier.isValid(this.jigsawStartIn.getText());
                    break;
            }
        } catch(NumberFormatException e) {
            this.doneBtn.active = false;
        }
    }

    private void setInitialModeSpecificState() {
        StructureRepeaterBlockEntity.RepeaterData data = this.backingBe.getData();
        StructureRepeaterBlockEntity.Mode mode = this.backingBe.getMode();
        switch(mode) {
            case SINGLE:
                StructureRepeaterBlockEntity.Single single = data.asSingle();
                this.singleFillIn.setText(ExtendedStructureHandling.stringifyBlockState(single.fill));
                if(single.base != single.fill) {
                    this.singleBaseIn.setText(ExtendedStructureHandling.stringifyBlockState(single.base));
                }
                if(single.cap != single.fill) {
                    this.singleCapIn.setText(ExtendedStructureHandling.stringifyBlockState(single.cap));
                }
                break;
            case LAYER:
                break;
            case JIGSAW:
                this.jigsawStartIn.setText(data.asJigsaw().startPool.toString());
                break;
        }
        this.changeMode(mode);
    }

    private void changeMode(StructureRepeaterBlockEntity.Mode mode) {
        @SuppressWarnings("unchecked")
        var children = (List<Element>)this.children();
        switch(mode) {
            case SINGLE -> {
                children.set(this.modeSpecificStart, this.singleFillIn);
                children.set(this.modeSpecificStart + 1, this.singleBaseIn);
                children.set(this.modeSpecificStart + 2, this.singleCapIn);
            }
            case LAYER -> {
                children.set(this.modeSpecificStart, this.dummy);
                children.set(this.modeSpecificStart + 1, this.dummy);
                children.set(this.modeSpecificStart + 2, this.dummy);
            }
            case JIGSAW -> {
                children.set(this.modeSpecificStart, this.jigsawStartIn);
                children.set(this.modeSpecificStart + 1, this.dummy);
                children.set(this.modeSpecificStart + 2, this.dummy);
            }
        }
        this.updateDoneButton();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        // title
        DrawableHelper.drawCenteredText(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.repeater.title"),
            this.centerH,
            this.centerV - (11 * WIDGET_HEIGHT / 2),
            0xffffff);
        renderLabeledWidget(matrices, mouseX, mouseY, delta, "min_repeat", this.minRepeatIn);
        renderLabeledWidget(matrices, mouseX, mouseY, delta, "max_repeat", this.maxRepeatIn);
        renderLabeledWidget(matrices, mouseX, mouseY, delta, "replacement", this.replacement);
        // mode specific
        switch(this.modeIn.getValue()) {
            case SINGLE:
                renderLabeledWidget(matrices, mouseX, mouseY, delta, "mode.single.base", this.singleBaseIn);
                renderLabeledWidget(matrices, mouseX, mouseY, delta, "mode.single.fill", this.singleFillIn);
                renderLabeledWidget(matrices, mouseX, mouseY, delta, "mode.single.cap", this.singleCapIn);
                break;
            case LAYER:
                break;
            case JIGSAW:
                renderLabeledWidget(matrices, mouseX, mouseY, delta, "mode.jigsaw.start", this.jigsawStartIn);
                break;
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderLabeledWidget(MatrixStack matrices, int mouseX, int mouseY, float delta, String s, TextFieldWidget widget) {
        DrawableHelper.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.repeater." + s),
            widget.x,
            widget.y - WIDGET_HEIGHT / 2,
            0xa0a0a0);
        widget.render(matrices, mouseX, mouseY, delta);
    }

    private void sendDataToServer() {
        String data1, data2, data3;
        switch(this.modeIn.getValue()) {
            case SINGLE -> {
                data1 = this.singleFillIn.getText();
                data2 = this.singleBaseIn.getText();
                data3 = this.singleCapIn.getText();
                if(data2.equals(data1)) {
                    data2 = "";
                }
                if(data3.equals(data1)) {
                    data3 = "";
                }
            }
            case LAYER -> {
                data1 = "minecraft:empty";
                data2 = data3 = "";
            }
            case JIGSAW -> {
                data1 = this.jigsawStartIn.getText();
                data2 = data3 = "";
            }
            default -> throw new AssertionError(this.modeIn.getValue());
        }
        RepeaterPacketData data =
            new RepeaterPacketData(backingBe.getPos(),
                Integer.parseInt(this.minRepeatIn.getText()),
                Integer.parseInt(this.maxRepeatIn.getText()),
                this.stopAtSolidBtn.getValue(),
                this.replacement.getText(),
                this.modeIn.getValue(),
                data1,
                data2,
                data3);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        data.write(buf);
        ClientPlayNetworking.send(ServerStructHelpPackets.REPEATER_UPDATE, buf);
        this.onClose();
    }
}
