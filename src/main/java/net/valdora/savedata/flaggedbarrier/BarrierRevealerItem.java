package net.valdora.savedata.flaggedbarrier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class BarrierRevealerItem extends Item {
    public BarrierRevealerItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("Barrier Revealer");
    }
}