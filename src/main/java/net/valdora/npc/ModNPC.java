package net.valdora.npc;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.npc.commands.SpawnNPCCommand;
import net.valdora.npc.custom.StaticNPC;
import net.valdora.spawning.commands.DebugValidSpawnsCommand;
import net.valdora.spawning.commands.ReloadConfigCommand;
import net.valdora.spawning.commands.ReloadSpawnPoolsCommand;

public class ModNPC {
    public static EntityType<StaticNPC> STATIC_NPC;

    public static void registerEntities() {
        STATIC_NPC = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(Valdora.MOD_ID, "static_npc"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, StaticNPC::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                        .build()
        );

        FabricDefaultAttributeRegistry.register(
                STATIC_NPC,
                StaticNPC.createAttributes()
        );

        Valdora.LOGGER.info("Registered Static NPC");

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            SpawnNPCCommand.register(dispatcher);
        }));
    }
}