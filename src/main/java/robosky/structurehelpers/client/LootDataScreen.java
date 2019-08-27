package robosky.structurehelpers.client;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.client.ClientCottonScreen;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WTextField;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.network.UpdateLootDataC2SPacket;

/**
 * Screen class for the loot data block entity.
 */
public class LootDataScreen extends ClientCottonScreen {

    public LootDataScreen(LootDataBlockEntity be) {
        super(new Description(be));
    }

    /**
     * GUI for the loot data block entity.
     */
    private static final class Description extends LightweightGuiDescription {

        private final LootDataBlockEntity backingBe;

        private final WTextField lootTableIn;

        private final WTextField replaceIn;

        private final WButton done;

        Description(LootDataBlockEntity be) {
            backingBe = be;
            WGridPanel root = new WGridPanel(4);
            this.setRootPanel(root);

            WLabel title = new WLabel(new TranslatableText("gui.structure-helpers.loot_data.title"), WLabel.DEFAULT_TEXT_COLOR);
            root.add(title, 0, 0, 60, 1);

            WLabel lootTableLbl = new WLabel(new TranslatableText("gui.structure-helpers.loot_data.table"), WLabel.DEFAULT_TEXT_COLOR);
            root.add(lootTableLbl, 0, 7, 60, 1);
            lootTableIn = new WTextField();
            lootTableIn.createPeers(this); // required to make text field focusable
            lootTableIn.setMaxLength(4096);
            lootTableIn.setText(be.getLootTable().toString());
            root.add(lootTableIn, 0, 10, 60, 1);

            WLabel replaceLbl = new WLabel(new TranslatableText("gui.structure-helpers.loot_data.replace"), WLabel.DEFAULT_TEXT_COLOR);
            root.add(replaceLbl, 0, 17, 60, 1);
            replaceIn = new WTextField();
            replaceIn.createPeers(this); // required to make text field focusable
            replaceIn.setMaxLength(4096);
            replaceIn.setText(be.getReplacementState());
            root.add(replaceIn, 0, 20, 60, 1);

            done = new WButton(new TranslatableText("gui.done"));
            done.setOnClick(this::sendDataToServer);
            root.add(done, 0, 31, 29, 1);
            WButton cancel = new WButton(new TranslatableText("gui.cancel"));
            cancel.setOnClick(() -> MinecraftClient.getInstance().openScreen(null));
            root.add(cancel, 31, 31, 29, 1);

            // disable saving when the loot table ID is not a valid Identifier
            lootTableIn.setChangedListener(text -> done.setEnabled(Identifier.isValid(text)));
        }

        private void sendDataToServer() {
            UpdateLootDataC2SPacket packet =
                new UpdateLootDataC2SPacket(backingBe.getPos(), lootTableIn.getText(), replaceIn.getText());
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
            MinecraftClient.getInstance().openScreen(null);
        }
    }
}
