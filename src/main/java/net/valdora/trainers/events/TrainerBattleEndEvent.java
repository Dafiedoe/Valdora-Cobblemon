package net.valdora.trainers.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.mojang.brigadier.ParseResults;
import kotlin.Unit;
import net.minecraft.server.command.ServerCommandSource;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.utils.PokemonTeamBattleActor;
import java.util.UUID;

public class TrainerBattleEndEvent {
    public static void register() {
        CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, TrainerBattleEndEvent::battleFled);
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, TrainerBattleEndEvent::battleFaint);
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, TrainerBattleEndEvent::battleVictory);
    }

    private static Unit battleFled(BattleFledEvent battleFledEvent) {
        PokemonBattle battle = battleFledEvent.getBattle();
        battleLost(battle);
        return null;
    }

    private static Unit battleFaint(BattleFaintedEvent battleFaintedEvent) {
        PokemonBattle battle = battleFaintedEvent.getBattle();

        PlayerBattleActor playerActor = null;
        BattleSide firstSide = battle.getSide1();
        BattleSide secondSide = battle.getSide2();

        if (firstSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            playerActor = playerBattleActor;
        } else if (secondSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            playerActor = playerBattleActor;
        }

        if (playerActor != null && isPlayerSideDefeated(playerActor)) {
            battleLost(battle);
        }

        return null;
    }

    private static boolean isPlayerSideDefeated(PlayerBattleActor playerActor) {
        return playerActor.getPokemonList().stream().allMatch(pokemon -> pokemon.getHealth() <= 0);
    }

    private static void battleLost(PokemonBattle battle) {
        BattleSide firstSide = battle.getSide1();
        BattleSide secondSide = battle.getSide2();

        if (firstSide.getActors()[0] instanceof PokemonTeamBattleActor pokemonBattleActor &&
                secondSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            UUID playerUuid = playerBattleActor.getEntity().getUuid();
            PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(playerUuid);
            String flag = pokemonBattleActor.getId().toLowerCase();
            progress.setFlag(flag, "defeat");
            PlayerSaveDataManager.INSTANCE.saveProgress(playerUuid);

            String command = "easy_npc dialog open " + pokemonBattleActor.getNpcUuid() + " " + playerBattleActor.getEntity().getName().getString() + " " + flag + "_ondefeat";
            ServerCommandSource source = playerBattleActor.getEntity().getServer().getCommandSource().withLevel(2);
            try {
                ParseResults<ServerCommandSource> parseResults = playerBattleActor.getEntity().getServer().getCommandManager().getDispatcher().parse(command, source);
                playerBattleActor.getEntity().getServer().getCommandManager().execute(parseResults, command);
            } catch (Exception e) {
                Valdora.LOGGER.error("Failed to execute command: {}", command, e);
            }
        }
        if (secondSide.getActors()[0] instanceof PokemonTeamBattleActor pokemonBattleActor &&
                firstSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            UUID playerUuid = playerBattleActor.getEntity().getUuid();
            PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(playerUuid);
            String flag = pokemonBattleActor.getId().toLowerCase();
            progress.setFlag(flag, "defeat");
            PlayerSaveDataManager.INSTANCE.saveProgress(playerUuid);

            String command = "easy_npc dialog open " + pokemonBattleActor.getNpcUuid() + " " + playerBattleActor.getEntity().getName().getString() + " " + flag + "_ondefeat";
            ServerCommandSource source = playerBattleActor.getEntity().getServer().getCommandSource().withLevel(2);
            try {
                ParseResults<ServerCommandSource> parseResults = playerBattleActor.getEntity().getServer().getCommandManager().getDispatcher().parse(command, source);
                playerBattleActor.getEntity().getServer().getCommandManager().execute(parseResults, command);
            } catch (Exception e) {
                Valdora.LOGGER.error("Failed to execute command: {}", command, e);
            }
        }
    }

    private static Unit battleVictory(BattleVictoryEvent battleVictoryEvent) {
        PokemonBattle battle = battleVictoryEvent.getBattle();

        boolean playerWon = false;
        BattleSide firstSide = battle.getSide1();
        BattleSide secondSide = battle.getSide2();
        PlayerBattleActor playerActor = null;

        if (firstSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            playerActor = playerBattleActor;
        } else if (secondSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            playerActor = playerBattleActor;
        }

        if (playerActor != null && battleVictoryEvent.getWinners().contains(playerActor)) {
            playerWon = true;
        }

        if (playerWon) {
            battleWon(battle);
        } else {
            battleLost(battle);
        }

        return null;
    }

    private static void battleWon(PokemonBattle battle) {
        BattleSide firstSide = battle.getSide1();
        BattleSide secondSide = battle.getSide2();

        if (firstSide.getActors()[0] instanceof PokemonTeamBattleActor pokemonBattleActor &&
                secondSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            UUID playerUuid = playerBattleActor.getEntity().getUuid();
            PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(playerUuid);
            String flag = pokemonBattleActor.getId().toLowerCase();
            progress.setFlag(flag, "victory");
            PlayerSaveDataManager.INSTANCE.saveProgress(playerUuid);

            String command = "easy_npc dialog open " + pokemonBattleActor.getNpcUuid() + " " + playerBattleActor.getEntity().getName().getString() + " " + flag + "_onvictory";
            ServerCommandSource source = playerBattleActor.getEntity().getServer().getCommandSource().withLevel(2);
            try {
                ParseResults<ServerCommandSource> parseResults = playerBattleActor.getEntity().getServer().getCommandManager().getDispatcher().parse(command, source);
                playerBattleActor.getEntity().getServer().getCommandManager().execute(parseResults, command);
            } catch (Exception e) {
                Valdora.LOGGER.error("Failed to execute command: {}", command, e);
            }
        }
        if (secondSide.getActors()[0] instanceof PokemonTeamBattleActor pokemonBattleActor &&
                firstSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
            UUID playerUuid = playerBattleActor.getEntity().getUuid();
            PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(playerUuid);
            String flag = pokemonBattleActor.getId().toLowerCase();
            progress.setFlag(flag, "victory");
            PlayerSaveDataManager.INSTANCE.saveProgress(playerUuid);

            String command = "easy_npc dialog open " + pokemonBattleActor.getNpcUuid() + " " + playerBattleActor.getEntity().getName().getString() + " " + flag + "_onvictory";
            ServerCommandSource source = playerBattleActor.getEntity().getServer().getCommandSource().withLevel(2);
            try {
                ParseResults<ServerCommandSource> parseResults = playerBattleActor.getEntity().getServer().getCommandManager().getDispatcher().parse(command, source);
                playerBattleActor.getEntity().getServer().getCommandManager().execute(parseResults, command);
                Valdora.LOGGER.info("Server executed command: {}", command);
            } catch (Exception e) {
                Valdora.LOGGER.error("Failed to execute command: {}", command, e);
            }
        }
    }
}