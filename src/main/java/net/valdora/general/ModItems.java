package net.valdora.general;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.items.repel.RepelItem;
import net.valdora.items.repel.RepelType;

public class ModItems {
    public static final Item REPEL = Registry.register(Registries.ITEM, Identifier.of(Valdora.MOD_ID, "repel"), new RepelItem(new Item.Settings(), RepelType.Default));
    public static final Item SUPER_REPEL = Registry.register(Registries.ITEM, Identifier.of(Valdora.MOD_ID, "super_repel"), new RepelItem(new Item.Settings(), RepelType.Super));
    public static final Item MAX_REPEL = Registry.register(Registries.ITEM, Identifier.of(Valdora.MOD_ID, "max_repel"), new RepelItem(new Item.Settings(), RepelType.Max));

    public static void register() {
//        ItemGroupEvents.modifyEntriesEvent(ModItemGroups.VALDORA_ITEMS_GROUP).register(entries -> {
//            entries.add(REPEL);
//        });
    }
}
