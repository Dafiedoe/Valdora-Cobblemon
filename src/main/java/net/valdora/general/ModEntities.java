package net.valdora.general;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.valdora.Valdora;
import net.valdora.trainers.DummyEntity;

public class ModEntities {
    public static EntityType<SurfBoardEntity> SURF_BOARD;
    
    public static EntityType<DummyEntity> DUMMY_ENTITY;
    
    public static void register() {
        SURF_BOARD = FabricEntityTypeBuilder.<SurfBoardEntity>create(SpawnGroup.MISC, SurfBoardEntity::new).dimensions(EntityDimensions.fixed(1.5f, 0.6f)).build();
        Registry.register(Registries.ENTITY_TYPE, Identifier.of(Valdora.MOD_ID, "surf_board"), SURF_BOARD);
        
        DUMMY_ENTITY = FabricEntityTypeBuilder.<DummyEntity>create(SpawnGroup.MISC, DummyEntity::new).dimensions(EntityDimensions.fixed(0.0f, 0.0f)).trackedUpdateRate(Integer.MAX_VALUE)
                .forceTrackedVelocityUpdates(false).build();
        Registry.register(Registries.ENTITY_TYPE, Identifier.of(Valdora.MOD_ID, "dummy"), DUMMY_ENTITY);
        
        FabricDefaultAttributeRegistry.register(DUMMY_ENTITY, DummyEntity.createAttributes());
    }
}
