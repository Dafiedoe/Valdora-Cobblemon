package net.valdora.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import kotlin.Unit;
import net.valdora.Valdora;
import net.minecraft.entity.Entity;

public class DeletePokemonAfterBattleEvent {
    public static void register() {
        CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, DeletePokemonAfterBattleEvent::battleFled);
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, DeletePokemonAfterBattleEvent::battleFaint);
    }

    private static Unit battleFled(BattleFledEvent battleFledEvent) {
        PokemonBattle battle = battleFledEvent.getBattle();

        battleEnd(battle);

        return null;
    }

    private static Unit battleFaint(BattleFaintedEvent battleFaintedEvent) {
        PokemonBattle battle = battleFaintedEvent.getBattle();

        battleEnd(battle);

        return null;
    }

    private static void battleEnd(PokemonBattle battle) {
        BattleSide firstSide = battle.getSide1();
        BattleSide secondSide = battle.getSide2();

        if (firstSide.getActors()[0] instanceof PokemonBattleActor pokemonBattleActor) {
            pokemonBattleActor.getEntity().remove(Entity.RemovalReason.DISCARDED);
        }
        if (secondSide.getActors()[0] instanceof PokemonBattleActor pokemonBattleActor) {
            pokemonBattleActor.getEntity().remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
