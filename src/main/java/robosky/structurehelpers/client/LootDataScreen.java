package robosky.structurehelpers.client;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.client.ClientCottonScreen;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WTextField;

import net.minecraft.text.TranslatableText;

import robosky.structurehelpers.block.LootDataBlockEntity;

/**
 * Screen class for the loot data block entity.
 */
public class LootDataScreen extends ClientCottonScreen {

    private final LootDataBlockEntity backingBe;

    public LootDataScreen(LootDataBlockEntity be) {
        super(new Description());
        backingBe = be;
    }

    private static final class Description extends LightweightGuiDescription {

        Description() {
            WGridPanel root = new WGridPanel(4);
            this.setRootPanel(root);

            WLabel lootTableLbl = new WLabel(new TranslatableText("gui.structure-helpers.loot_data.table"), WLabel.DEFAULT_TEXT_COLOR);
            root.add(lootTableLbl, 0, 0, 40, 1);
            WTextField lootTableIn = new WTextField();
            lootTableIn.createPeers(this); // required to make text field focusable
            lootTableIn.setMaxLength(4096);
            root.add(lootTableIn, 0, 3, 40, 1);

            WLabel replaceLbl = new WLabel(new TranslatableText("gui.structure-helpers.loot_data.replace"), WLabel.DEFAULT_TEXT_COLOR);
            root.add(replaceLbl, 0, 10, 40, 1);
            WTextField replaceIn = new WTextField();
            replaceIn.createPeers(this); // required to make text field focusable
            replaceIn.setMaxLength(4096);
            root.add(replaceIn, 0, 13, 40, 1);

            WButton done = new WButton(new TranslatableText("gui.done"));
            done.setOnClick(() -> {});
            root.add(done, 0, 24, 19, 1);
            WButton cancel = new WButton(new TranslatableText("gui.cancel"));
            done.setOnClick(() -> {});
            root.add(cancel, 21, 24, 19, 1);
        }
    }
}
