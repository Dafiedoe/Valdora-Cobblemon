package net.valdora.general;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.savedata.flaggedbarrier.FlaggedBarrierBlock;
import net.valdora.savedata.flaggedbarrier.FlaggedBarrierItem;

public class ModBlocks {
    public static final Block FLAGGED_BARRIER = new FlaggedBarrierBlock(
            FabricBlockSettings
                    .create()
                    .mapColor(MapColor.CLEAR)
                    .strength(-1.0F, 3600000.0F) // unbreakable
                    .noBlockBreakParticles()
                    .nonOpaque()
                    .luminance(state -> 0)
                    .sounds(BlockSoundGroup.STONE)
    );

    public static void register() {
        registerBlock("flagged_barrier", FLAGGED_BARRIER);
    }

    private static void registerBlock(String name, Block block) {
        Identifier id = Identifier.of(Valdora.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new FlaggedBarrierItem(block, new Item.Settings()));
    }
}
