package net.valdora.general;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.valdora.general.SurfBoardEntity;

public class ModEntities {
    public static EntityType<SurfBoardEntity> SURF_BOARD;

    public static void register() {
        SURF_BOARD = FabricEntityTypeBuilder
                .<SurfBoardEntity>create(SpawnGroup.MISC, SurfBoardEntity::new)
                .dimensions(EntityDimensions.fixed(1.5f, 0.6f))
                .build();
        Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of("valdora", "surf_board"),
                SURF_BOARD
        );
        EntityRendererRegistry.register(
                SURF_BOARD,
                context -> new BoatEntityRenderer(context, false)
        );
    }
}
