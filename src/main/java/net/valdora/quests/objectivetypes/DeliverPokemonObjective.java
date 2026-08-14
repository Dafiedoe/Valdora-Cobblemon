package net.valdora.quests.objectivetypes;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.Valdora;
import net.valdora.quests.ActiveQuest;
import net.valdora.quests.Objective;
import net.valdora.quests.ObjectiveType;

public class DeliverPokemonObjective extends Objective {
    private Species species;
    private ElementalType type;
    private boolean any = false;
    
    public DeliverPokemonObjective(String title, String description, String questId, JsonObject json) {
        super(title, description, ObjectiveType.DELIVER_POKEMON, questId);
        
        if (json.has("species")) {
            String speciesStr = json.get("species").getAsString().trim();
            if (speciesStr.equalsIgnoreCase("any")) {
                any = true;
            } else {
                species = PokemonSpecies.INSTANCE.getByName(speciesStr.toLowerCase());
                if (species == null) {
                    Valdora.LOGGER.error("DeliverPokemonObjective: Pokemon Species '" + speciesStr + "' not found for quest " + questId);
                }
            }
        }
        
        if (json.has("pkmn_type")) {
            String typeStr = json.get("pkmn_type").getAsString().trim();
            if (typeStr.equalsIgnoreCase("any")) {
                any = true;
            } else {
                type = ElementalTypes.INSTANCE.get(typeStr.toLowerCase());
                if (type == null) {
                    Valdora.LOGGER.error("DeliverPokemonObjective: Pokemon Type '" + typeStr + "' not found for quest " + questId);
                }
            }
        }
        
        if (species != null && type != null) {
            Valdora.LOGGER.warn("DeliverPokemonObjective: Both 'species' and 'pkmn_type' provided for quest " + questId + ". Species will take precedence.");
        }
        
        if (!any && species == null && type == null) {
            Valdora.LOGGER.warn("DeliverPokemonObjective: No valid species/type/any configured for quest " + questId + ".");
        }
    }
    
    @Override
    public boolean handleObjectiveUpdate(ActiveQuest activeQuest, ServerPlayerEntity player, Object data) {
        if (!any && species == null && type == null) {
            return false;
        }
        
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        if (party == null) return false;
        
        int amount = 0;
        for (int i = 0; i < party.size(); i++) {
            Pokemon pokemon = party.get(i);
            if (pokemon == null) continue;
            
            if (any) {
                amount++;
            } else if (species != null) {
                if (species.equals(pokemon.getSpecies())) {
                    amount++;
                }
            } else {
                ElementalType pPrim = pokemon.getPrimaryType();
                ElementalType pSec = pokemon.getSecondaryType();
                if ((pPrim != null && pPrim.equals(type)) || (pSec != null && pSec.equals(type))) {
                    amount++;
                }
            }
        }
        
        int remainingNeeded = count - activeQuest.count;
        if (remainingNeeded <= 0) {
            return activeQuest.count >= count;
        }
        
        int delivered = Math.min(amount, remainingNeeded);
        if (delivered <= 0) {
            return false;
        }
        
        int toRemove = delivered;
        for (int i = party.size() - 1; i >= 0 && toRemove > 0; i--) {
            Pokemon pokemon = party.get(i);
            if (pokemon == null) continue;
            
            boolean matches = false;
            if (any) {
                matches = true;
            } else if (species != null) {
                matches = species.equals(pokemon.getSpecies());
            } else if (type != null) {
                ElementalType pPrim = pokemon.getPrimaryType();
                ElementalType pSec = pokemon.getSecondaryType();
                matches = (pPrim != null && pPrim.equals(type)) || (pSec != null && pSec.equals(type));
            }
            
            if (matches) {
                party.remove(pokemon);
                toRemove--;
            }
        }
        
        activeQuest.count += delivered;
        return activeQuest.count >= count;
    }
}
