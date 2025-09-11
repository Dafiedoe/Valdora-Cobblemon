package net.valdora.savedata.flaggedbarrier;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.valdora.general.ModComponents;

public class FlaggedBarrierItem extends BlockItem {
    public FlaggedBarrierItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        String flag = stack.getOrDefault(ModComponents.FLAG_NAME, "");
        String value = stack.getOrDefault(ModComponents.FLAG_VALUE, "");
        return Text.literal("Flagged Barrier " + flag + " " + value);
    }
}