package net.valdora.utils;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.valdora.spawning.SpawnEntry;
import net.valdora.spawning.SpawnPoolManager;
import net.valdora.Valdora;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class PokemonUtils {
    public static boolean isPlayerInBattle(ServerPlayerEntity player) {
        return BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player) != null;
    }

    public static PokemonEntity spawnWildPokemon(ServerPlayerEntity player) {
        World world = player.getWorld();
        Vec3d pos = player.getPos();
        BlockPos blockPos = BlockPos.ofFloored(pos);

        List<SpawnEntry> validSpawns = SpawnPoolManager.getValidSpawnsForPlayer(player);

        if (validSpawns.isEmpty()) {
            Valdora.LOGGER.info("There are no spawns available for player " + player.getName() + " at " + player.getPos().x + " " + player.getPos().y + " " + player.getPos().z);
            return null;
        }

        int totalWeight = validSpawns.stream().mapToInt(entry -> entry.spawn_weight).sum();

        Random random = new Random();
        int randomWeight = random.nextInt(totalWeight);

        int cumulativeWeight = 0;
        SpawnEntry selectedEntry = null;

        for (SpawnEntry entry : validSpawns) {
            cumulativeWeight += entry.spawn_weight;
            if (randomWeight < cumulativeWeight) {
                selectedEntry = entry;
                break;
            }
        }

        if (selectedEntry == null) {
            Valdora.LOGGER.info("SpawnWeight was out of bounds!");
            return null;
        }

        String pokemonName = selectedEntry.pokemon;
        int minLevel = selectedEntry.min_level;
        int maxLevel = selectedEntry.max_level;

        int level = random.nextInt(maxLevel - minLevel + 1) + minLevel;

        PokemonEntity pokemonEntity = PokemonProperties.Companion.parse(pokemonName.toLowerCase() + " level=" + level).createEntity(world);

        pokemonEntity.setPosition(blockPos.toCenterPos());

        return pokemonEntity;
    }

    public static void startWildBattle(ServerPlayerEntity player) {
        World world = player.getWorld();

        if (!(world instanceof ServerWorld)) return;

        PokemonEntity wildPokemonEntity = spawnWildPokemon(player);
        if (wildPokemonEntity == null) return;

        world.spawnEntity(wildPokemonEntity);

        TickScheduler.runNextTick(2, () -> {
            BattleBuilder.INSTANCE.pve(player, wildPokemonEntity);
        });
    }
}
