package net.valdora.general;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public class ModItemGroups {
    public static final ItemGroup VALDORA_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Valdora.MOD_ID, "valdora_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.REPEL))
                    .displayName(Text.translatable("itemgroup.valdora.valdora_items"))
                    .entries(((displayContext, entries) -> {
                        entries.add(ModItems.REPEL);
                        entries.add(ModItems.SUPER_REPEL);
                        entries.add(ModItems.MAX_REPEL);
                    })).build());

    public static void register() {

    }
}
