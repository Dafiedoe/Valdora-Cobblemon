package net.valdora.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.ai.RandomBattleAI;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.*;
import kotlin.Unit;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.valdora.spawning.SpawnEntry;
import net.valdora.spawning.SpawnPoolManager;
import net.valdora.Valdora;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.valdora.trainers.ConfigPokemon;
import net.valdora.trainers.TrainerConfig;
import net.valdora.trainers.TrainerManager;

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
            Valdora.LOGGER.info("Player named '" + player.getName() + "' has no pokemon to battle with.");
            return false;
        }
        return true;
    }

    public static void startTrainerBattle(ServerPlayerEntity player, String trainerId) {
        if (!hasPokemonAvailable(player)) {
            player.sendMessage(Text.literal("You need at least one Pokémon to battle!"), false);
            return;
        }

        TrainerConfig trainer = TrainerManager.getTrainerById(trainerId);

        if (trainer == null) {
            player.sendMessage(Text.literal("Invalid trainer id"));
            return;
        }

        try {
            final BattleFormat battleFormat = switch (trainer.battleFormat.toLowerCase()) {
                case "singles" -> BattleFormat.Companion.getGEN_9_SINGLES();
                case "doubles" -> BattleFormat.Companion.getGEN_9_DOUBLES();
                case "triples" -> BattleFormat.Companion.getGEN_9_TRIPLES();
                case "multi" -> BattleFormat.Companion.getGEN_9_MULTI();
                case "royal" -> BattleFormat.Companion.getGEN_9_ROYAL();
                default -> BattleFormat.Companion.getGEN_9_SINGLES();
            };

            List<BattlePokemon> trainerTeam = new ArrayList<>();

            for (ConfigPokemon configPokemon : trainer.pokemonTeam) {
                Pokemon builtPkmn = configPokemon.build();
                if (builtPkmn == null) {
                    Valdora.LOGGER.error("Failed to build Pokémon");
                    return;
                }
                BattlePokemon battlePokemon = new BattlePokemon(
                        builtPkmn,
                        builtPkmn.clone(true, player.getServer().getRegistryManager()),
                        pokemonEntity -> {
                            pokemonEntity.discard();
                            return Unit.INSTANCE;
                        }
                );
                battlePokemon.getEffectedPokemon().setCurrentHealth(battlePokemon.getEffectedPokemon().getMaxHealth());
                trainerTeam.add(battlePokemon);
            }

            if (trainerTeam.isEmpty()) {
                Valdora.LOGGER.error("Trainer team is empty!");
                player.sendMessage(Text.literal("Trainer has no Pokémon!"), false);
                return;
            }

            BattleAI battleAI = null;
            if (trainer.aiLevel >= 0) battleAI = new StrongBattleAI(trainer.aiLevel);
            else battleAI = new RandomBattleAI();
            PokemonTeamBattleActor trainerActor = new PokemonTeamBattleActor(trainer.trainerName, UUID.randomUUID(), trainerTeam, battleAI);

            trainerTeam.forEach(p -> p.setActor(trainerActor));

            PlayerPartyStore playerParty = Cobblemon.INSTANCE.getStorage().getParty(player);
            List<BattlePokemon> playerTeam = playerParty.toBattleTeam();

            if (playerTeam.isEmpty()) {
                Valdora.LOGGER.error("Player team is empty!");
                return;
            }

            PlayerBattleActor playerActor = new PlayerBattleActor(player.getUuid(), playerTeam);

            TickScheduler.runNextTick(2, () -> {
                playerParty.toGappyList().stream()
                        .filter(Objects::nonNull)
                        .forEach(Pokemon::recall);

                BattleSide trainerSide = new BattleSide(trainerActor);
                BattleSide playerSide = new BattleSide(playerActor);

                try {
                    trainerTeam.get(0).getEffectedPokemon().sendOut(player.getServerWorld(), player.getPos(), null, pokemonEntity -> { return Unit.INSTANCE; });
                    playerTeam.get(0).getEffectedPokemon().sendOut(player.getServerWorld(), player.getPos(), null, pokemonEntity -> { return Unit.INSTANCE; });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Cobblemon.INSTANCE.getBattleRegistry().startBattle(
                        battleFormat,
                        playerSide,
                        trainerSide,
                        false
                );
                Valdora.LOGGER.info("Battle started successfully");
            });
        } catch (Exception e) {
            Valdora.LOGGER.error("Error starting trainer battle: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(Text.literal("An error occurred while starting the battle."), false);
        }
    }
}
