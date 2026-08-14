package net.valdora.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.valdora.battle.AbstractPlayerBattleParticipant;
import net.valdora.battle.AbstractTrainerBattle;
import net.valdora.battle.AbstractTrainerBattleParticipant;
import net.valdora.battle.TrainerBattle;
import net.valdora.battle.exception.BattleStartException;
import net.valdora.general.ModEntities;
import net.valdora.spawning.SpawnEntry;
import net.valdora.spawning.SpawnPoolManager;
import net.valdora.Valdora;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.valdora.timespecificevents.ShinyHour;
import net.valdora.trainers.*;

import java.util.*;

public class PokemonUtils {
    public static boolean isPlayerInBattle(ServerPlayerEntity player) {
        return BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player) != null;
    }
    
    public static PokemonEntity spawnWildPokemon(ServerPlayerEntity player) {
        if (!hasPokemonAvailable(player)) return null;
        
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
        pokemonEntity.getPokemon().setShiny(rollForShiny(player));
        
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
    
    public static boolean hasPokemon(ServerPlayerEntity player) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        boolean hasPokemon = false;
        for (int i = 0; i < party.size(); i++) {
            Pokemon pokemon = party.get(i);
            if (pokemon != null) {
                hasPokemon = true;
                break;
            }
        }
        return hasPokemon;
    }
    
    public static boolean hasPokemonAvailable(ServerPlayerEntity player) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        boolean hasAvailablePkmn = false;
        for (int i = 0; i < party.size(); i++) {
            Pokemon pokemon = party.get(i);
            if (pokemon != null && !pokemon.isFainted()) {
                hasAvailablePkmn = true;
            }
        }
        if (!hasAvailablePkmn) {
            return false;
        }
        return true;
    }
    
    public static void startTrainerBattle(ServerPlayerEntity player, String trainerId, String trainerNpcUuid) {
        if (!hasPokemonAvailable(player)) {
            player.sendMessage(Text.literal("You need at least one Pokémon to battle!"), false);
            return;
        }
        
        if (isPlayerInBattle(player)) {
            Valdora.LOGGER.info(player.getName().getString() + " is already in battle!");
            return;
        }
        
        TrainerConfig trainer = TrainerManager.getTrainerById(trainerId);
        
        DummyEntity dummy = new DummyEntity(ModEntities.DUMMY_ENTITY, player.getWorld());
        dummy.setPos(Math.floor(player.getX()) + 0.5, Math.floor(player.getY()) + 0.5, Math.floor(player.getZ()) + 0.5);
        player.getServerWorld().spawnEntity(dummy);
        
        TickScheduler.runNextTick(1, () -> {
            try {
                TrainerBattle trainerBattle = new AbstractTrainerBattle(new AbstractPlayerBattleParticipant(player, Cobblemon.INSTANCE.getStorage().getParty(player)),
                        new AbstractTrainerBattleParticipant(trainer, player, dummy, trainerNpcUuid));
                
                trainerBattle.start();
            } catch (BattleStartException e) {
                return;
            }
        });
    }
    
    public static boolean rollForShiny(ServerPlayerEntity player) {
        int roll = randomRoll(player);
        return roll == 0;
    }
    
    private static int randomRoll(ServerPlayerEntity player) {
        World world = player.getWorld();
        BlockPos pos = player.getBlockPos();
        
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        Biome biome = biomeEntry.value();
        
        Identifier biomeId = world.getRegistryManager().get(RegistryKeys.BIOME).getId(biome);
        
        int rollOdds = Valdora.BASE_SHINY_CHANCE;
        
        if (ShinyHour.isActive()) {
            if (ShinyHour.getBiome().equalsIgnoreCase(biomeId.getPath())) {
                rollOdds = (int) Math.floor(1.0 / Valdora.SHINY_TIME_MULTIPLIER * rollOdds);
                if (ShinyHour.isSunday()) {
                    rollOdds = (int) Math.floor(1.0 / Valdora.SHINY_SUNDAY_MULTIPLIER * rollOdds);
                }
            }
        }
        
        return new Random().nextInt(rollOdds + 1);
    }
}
