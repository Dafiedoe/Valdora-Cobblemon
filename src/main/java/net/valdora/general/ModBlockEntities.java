package net.valdora.general;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.savedata.flaggedbarrier.FlaggedBarrierEntity;

public class ModBlockEntities {
    public static BlockEntityType<FlaggedBarrierEntity> FLAGGED_BARRIER_ENTITY;

    public static void register() {
        FLAGGED_BARRIER_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(Valdora.MOD_ID, "flagged_barrier_entity"),
                FabricBlockEntityTypeBuilder.create(FlaggedBarrierEntity::new, ModBlocks.FLAGGED_BARRIER).build()
        );
    }
}
