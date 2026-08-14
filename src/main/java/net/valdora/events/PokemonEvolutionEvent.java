package net.valdora.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionAcceptedEvent;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import kotlin.Unit;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PokemonEvolutionEvent {
    private static final Map<UUID, ItemStack> evolutionItems = new HashMap<>();
    
    public static void register() {
        CobblemonEvents.EVOLUTION_ACCEPTED.subscribe(Priority.NORMAL, PokemonEvolutionEvent::evolutionAccepted);
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.NORMAL, PokemonEvolutionEvent::evolutionComplete);
    }
    
    private static Unit evolutionAccepted(EvolutionAcceptedEvent event) {
        evolutionItems.put(event.getPokemon().getUuid(), event.getPokemon().getHeldItem$common());
        
        return Unit.INSTANCE;
    }
    
    private static Unit evolutionComplete(EvolutionCompleteEvent event) {
        if (!evolutionItems.containsKey(event.getPokemon().getUuid())) return Unit.INSTANCE;
        
        event.getPokemon().setHeldItem$common(evolutionItems.get(event.getPokemon().getUuid()));
        
        evolutionItems.remove(event.getPokemon().getUuid());
        
        return Unit.INSTANCE;
    }
}
