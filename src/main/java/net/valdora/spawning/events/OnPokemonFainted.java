package net.valdora.spawning.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonFaintedEvent;
import kotlin.Unit;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.valdora.trainers.TrainerManager;

public class OnPokemonFainted {
    public static void register() {
        CobblemonEvents.POKEMON_FAINTED.subscribe(Priority.HIGHEST, OnPokemonFainted::PokemonFainted);
    }

    private static Unit PokemonFainted(PokemonFaintedEvent event) {
        if (event.getPokemon().getOriginalTrainer() != null && TrainerManager.getTrainerById(event.getPokemon().getOriginalTrainer()) != null) {
            event.getPokemon().setHeldItem$common(new ItemStack(Registries.ITEM.get(Identifier.of("minecraft:air"))));
        }

        return Unit.INSTANCE;
    }
}
