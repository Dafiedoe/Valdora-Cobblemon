package net.valdora.trainers.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.mojang.brigadier.ParseResults;
import kotlin.Unit;
import net.minecraft.server.command.ServerCommandSource;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.savedata.checkpoints.CheckPointManager;
import net.valdora.utils.PokemonTeamBattleActor;
import java.util.UUID;

public class TrainerBattleEndEvent {
    public static void register() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, TrainerBattleEndEvent::battleVictory);
    }

    private static Unit battleVictory(BattleVictoryEvent battleVictoryEvent) {
        PokemonBattle battle = battleVictoryEvent.getBattle();

        battleVictoryEvent.getLosers().stream()
                .filter(actor -> actor instanceof PlayerBattleActor)
                .map(actor -> ((PlayerBattleActor) actor).getEntity())
                .forEach(CheckPointManager::recallPlayerToCheckPoint);

        BattleActor actor1 = battle.getSide1().getActors()[0];
        BattleActor actor2 = battle.getSide2().getActors()[0];

        PlayerBattleActor playerActor = null;
        PokemonTeamBattleActor trainerActor = null;

        if (actor1 instanceof PlayerBattleActor && actor2 instanceof PokemonTeamBattleActor) {
            playerActor = (PlayerBattleActor) actor1;
            trainerActor = (PokemonTeamBattleActor) actor2;
        } else if (actor2 instanceof PlayerBattleActor && actor1 instanceof PokemonTeamBattleActor) {
            playerActor = (PlayerBattleActor) actor2;
            trainerActor = (PokemonTeamBattleActor) actor1;
        }

        if (playerActor != null && trainerActor != null) {
            boolean playerWon = battleVictoryEvent.getWinners().contains(playerActor);
            String status = playerWon ? "victory" : "defeat";
            String dialogSuffix = playerWon ? "_onvictory" : "_ondefeat";

            UUID playerUuid = playerActor.getEntity().getUuid();
            PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(playerActor.getEntity().getServer(), playerUuid);
            String flag = trainerActor.getId().toLowerCase();
            progress.setFlag(flag, status);
            PlayerSaveDataManager.INSTANCE.saveProgress(playerActor.getEntity().getServer(), playerUuid);

            String command = "easy_npc dialog open " + trainerActor.getNpcUuid() + " " + playerActor.getEntity().getName().getString() + " " + flag + dialogSuffix;
            ServerCommandSource source = playerActor.getEntity().getServer().getCommandSource().withLevel(2);
            try {
                ParseResults<ServerCommandSource> parseResults = playerActor.getEntity().getServer().getCommandManager().getDispatcher().parse(command, source);
                playerActor.getEntity().getServer().getCommandManager().execute(parseResults, command);
            } catch (Exception e) {
                Valdora.LOGGER.error("Failed to execute command: {}", command, e);
            }
        }

        return null;
    }
}