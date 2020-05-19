package robosky.structurehelpers.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import robosky.structurehelpers.block.SpawnDataBlockEntity;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.arguments.NbtCompoundTagArgumentType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * Screen class for the spawn data block entity.
 */
public class SpawnDataScreen extends Screen {

    private static final int guiRadius = 150;

    // relevant input fields
    private final SpawnDataBlockEntity backingBe;
    private TextFieldWidget entityTypeFld;
    private TextFieldWidget deathLootTableFld;
    // switch to custom toggle button class
    private ToggleButtonWidget canPickUpLootTgl;
    private ToggleButtonWidget noAiTgl;
    private ToggleButtonWidget persistentTgl;
    private SimpleSliderWidget leftHandedChanceSld;
    private TextFieldWidget paramsFld;
    private ButtonWidget doneBtn;

    // center screen position, for convenience
    private int centerH;
    private int centerV;

    public SpawnDataScreen(SpawnDataBlockEntity be) {
        super(NarratorManager.EMPTY);
        this.backingBe = be;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        final int padding = 5;
        final int btnWidth = guiRadius - padding + 1;
        centerH = this.width / 2;
        centerV = this.height / 2;
        assert this.client != null : "this.client null in SpawnDataScreen";
        this.client.keyboard.enableRepeatEvents(true);
        // entity type ID
        entityTypeFld = new TextFieldWidget(this.textRenderer,
            centerH - guiRadius,
            centerV - 40,
            guiRadius * 2,
            20,
            new TranslatableText("gui.structure-helpers.spawn_data.entity"));
        entityTypeFld.setMaxLength(4096);
        entityTypeFld.setText(Registry.ENTITY_TYPE.getId(backingBe.getEntityType()).toString());
        entityTypeFld.setChangedListener(text -> updateDoneButton());
        this.children.add(entityTypeFld);
        // death loot table
        deathLootTableFld = new TextFieldWidget(this.textRenderer,
            centerH - guiRadius,
            centerV,
            guiRadius * 2,
            20,
            new TranslatableText("gui.structure-helpers.spawn_data.death_loot_table"));
        deathLootTableFld.setMaxLength(4096);
        deathLootTableFld.setText(Registry.ENTITY_TYPE.getId(backingBe.getEntityType()).toString());
        deathLootTableFld.setChangedListener(text -> updateDoneButton());
        this.children.add(deathLootTableFld);
        // settings buttons
        canPickUpLootTgl = this.addButton(new ToggleButtonWidget(centerH - padding - btnWidth,
            centerV + 40,
            btnWidth,
            20,
            false));
        noAiTgl = this.addButton(new ToggleButtonWidget(centerH - padding - btnWidth,
            centerV + 80,
            btnWidth,
            20,
            false));
        persistentTgl = this.addButton(new ToggleButtonWidget(centerH - padding - btnWidth,
            centerV + 120,
            btnWidth,
            20,
            false));
        // left handed chance
        leftHandedChanceSld = new SimpleSliderWidget(this.centerH - guiRadius,
            this.centerV + 160,
            guiRadius * 2,
            20,
            new TranslatableText("gui.structure-helpers.spawn_data.left_handed_chance"),
            0.0);
        // parameters NBT
        paramsFld = new TextFieldWidget(this.textRenderer,
            centerH - guiRadius,
            centerV + 200,
            guiRadius * 2,
            20,
            new TranslatableText("gui.structure-helpers.spawn_data.params"));
        paramsFld.setMaxLength(4096);
        paramsFld.setText(backingBe.getParameters().toString());
        paramsFld.setChangedListener(text -> updateDoneButton());
        this.children.add(paramsFld);
        // done + cancel buttons
        doneBtn = this.addButton(new ButtonWidget(centerH - padding - btnWidth,
            centerV + 240,
            btnWidth,
            20,
            ScreenTexts.DONE,
            btn -> this.sendDataToServer()));
        this.addButton(new ButtonWidget(centerH + padding,
            centerV + 240,
            btnWidth,
            20,
            ScreenTexts.CANCEL,
            btn -> this.onClose()));
    }

    private void updateDoneButton() {
        boolean active = Identifier.isValid(entityTypeFld.getText());
        if(active) {
            try {
                NbtCompoundTagArgumentType.nbtCompound().parse(new StringReader(paramsFld.getText()));
            } catch(CommandSyntaxException e) {
                active = false;
            }
        }
        doneBtn.active = active;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        // title
        this.drawCenteredString(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.spawn_data.title"),
            centerH,
            centerV - 80,
            0xffffff);
        // entity type ID
        this.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.spawn_data.entity"),
            centerH - guiRadius,
            entityTypeFld.y - 10,
            0xa0a0a0);
        entityTypeFld.render(matrices, mouseX, mouseY, delta);
        // params NBT
        this.drawStringWithShadow(matrices,
            this.textRenderer,
            I18n.translate("gui.structure-helpers.spawn_data.params"),
            centerH - guiRadius,
            paramsFld.y - 10,
            0xa0a0a0);
        paramsFld.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void sendDataToServer() {
//        LootDataPacketData data =
//            new LootDataPacketData(backingBe.getPos(), entityTypeIn.getText(), paramsIn.getText());
//        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//        data.write(buf);
//        ClientSidePacketRegistry.INSTANCE.sendToServer(ServerStructHelpPackets.LOOT_DATA_UPDATE, buf);
        this.onClose();
    }

    private static class SimpleSliderWidget extends SliderWidget {

        private final Text baseMessage;

        public SimpleSliderWidget(int x, int y, int width, int height, Text message, double value) {
            super(x, y, width, height, message, value);
            this.baseMessage = message;
        }

        public double getValue() {
            return this.value;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(new LiteralText("").append(baseMessage).append(String.format("%d", (int)this.value * 100)));
        }

        @Override
        protected void applyValue() {
        }
    }
}
