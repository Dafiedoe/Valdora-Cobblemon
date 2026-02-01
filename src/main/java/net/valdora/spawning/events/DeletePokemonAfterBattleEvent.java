package net.valdora.spawning.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.valdora.Valdora;
import net.valdora.quests.ObjectiveType;
import net.valdora.quests.QuestManager;
import net.valdora.utils.PokemonTeamBattleActor;

public class DeletePokemonAfterBattleEvent {
    public static void register() {
        CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, DeletePokemonAfterBattleEvent::battleFled);
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, DeletePokemonAfterBattleEvent::battleFaint);
    }

    private static Unit battleFled(BattleFledEvent battleFledEvent) {
        PokemonBattle battle = battleFledEvent.getBattle();

        battleEnd(battle);

        return Unit.INSTANCE;
    }

    private static Unit battleFaint(BattleFaintedEvent battleFaintedEvent) {
        PokemonBattle battle = battleFaintedEvent.getBattle();

        battleEnd(battle);

        return Unit.INSTANCE;
    }

    private static void battleEnd(PokemonBattle battle) {
        BattleSide firstSide = battle.getSide1();
        BattleSide secondSide = battle.getSide2();

        if (firstSide.getActors()[0] instanceof PokemonTeamBattleActor teamBattleActor) {
            if (secondSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
                QuestManager.updateQuestProgress(playerBattleActor.getEntity(), ObjectiveType.DEFEAT_POKEMON, teamBattleActor.getActivePokemon().get(0).getBattlePokemon().getEffectedPokemon());
            }
            if (teamBattleActor.getEntity() != null) {
                teamBattleActor.getEntity().discard();
            }
        }
        if (secondSide.getActors()[0] instanceof PokemonTeamBattleActor teamBattleActor) {
            if (firstSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
                QuestManager.updateQuestProgress(playerBattleActor.getEntity(), ObjectiveType.DEFEAT_POKEMON, teamBattleActor.getActivePokemon().get(0).getBattlePokemon().getEffectedPokemon());
            }
            if (teamBattleActor.getEntity() != null) {
                teamBattleActor.getEntity().discard();
            }
        }

        if (firstSide.getActors()[0] instanceof PokemonBattleActor pokemonBattleActor) {
            if (secondSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
                QuestManager.updateQuestProgress(playerBattleActor.getEntity(), ObjectiveType.DEFEAT_POKEMON, pokemonBattleActor.getPokemon().getEffectedPokemon());
            }
            if (pokemonBattleActor.getEntity() != null) {
                pokemonBattleActor.getEntity().remove(Entity.RemovalReason.DISCARDED);
            }
        }
        if (secondSide.getActors()[0] instanceof PokemonBattleActor pokemonBattleActor) {
            if (firstSide.getActors()[0] instanceof PlayerBattleActor playerBattleActor) {
                QuestManager.updateQuestProgress(playerBattleActor.getEntity(), ObjectiveType.DEFEAT_POKEMON, pokemonBattleActor.getPokemon().getEffectedPokemon());
            }
            if (pokemonBattleActor.getEntity() != null) {
                pokemonBattleActor.getEntity().remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }
}
