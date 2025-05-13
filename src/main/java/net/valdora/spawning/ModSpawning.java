package net.valdora.spawning;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.valdora.spawning.commands.DebugValidSpawnsCommand;
import net.valdora.spawning.commands.ReloadConfigCommand;
import net.valdora.spawning.commands.ReloadSpawnPoolsCommand;
import net.valdora.spawning.events.DeletePokemonAfterBattleEvent;
import net.valdora.spawning.events.TallGrassWalkEvent;

public class ModSpawning {
    public static void registerSpawning() {
        TallGrassWalkEvent.register();
        DeletePokemonAfterBattleEvent.register();

        SpawnPoolManager.load();

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            ReloadConfigCommand.register(dispatcher);
            ReloadSpawnPoolsCommand.register(dispatcher);
            DebugValidSpawnsCommand.register(dispatcher);
        }));
    }
}
