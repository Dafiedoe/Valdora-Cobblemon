package net.valdora.shops;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public class ShopItem {
    public String item;
    public int cost;

    public ItemStack getItem() {
        Identifier id = Identifier.of(item);
        Item item = Registries.ITEM.get(id);
        if (item == null || item == ItemStack.EMPTY.getItem()) {
            Valdora.LOGGER.warn("Invalid shop item: " + item);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1);
    }
}
