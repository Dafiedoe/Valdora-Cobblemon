package net.valdora.spawning;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.valdora.spawning.commands.DebugValidSpawnsCommand;
import net.valdora.spawning.events.DeletePokemonAfterBattleEvent;
import net.valdora.spawning.events.ExpEvent;
import net.valdora.spawning.events.OnPokemonFainted;
import net.valdora.spawning.events.TallGrassWalkEvent;

public class ModSpawning {
    public static void registerSpawning() {
        TallGrassWalkEvent.register();
        DeletePokemonAfterBattleEvent.register();
        OnPokemonFainted.register();
        ExpEvent.register();
        
        SpawnPoolManager.load();
        
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            DebugValidSpawnsCommand.register(dispatcher);
        }));
    }
}
