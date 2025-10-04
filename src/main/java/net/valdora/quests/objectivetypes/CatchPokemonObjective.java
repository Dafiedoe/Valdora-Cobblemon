package net.valdora.quests.objectivetypes;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
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

public class CatchPokemonObjective extends Objective {
    private Species species;
    private ElementalType type;
    private boolean any = false;

    public CatchPokemonObjective(String title, String description, String questId, JsonObject json) {
        super(title, description, ObjectiveType.CATCH_POKEMON, questId);

        if (json.has("species")) {
            String speciesStr = json.get("species").getAsString().trim();
            if (speciesStr.equalsIgnoreCase("any")) {
                any = true;
            } else {
                species = PokemonSpecies.INSTANCE.getByName(speciesStr.toLowerCase());
                if (species == null) {
                    Valdora.LOGGER.error("CatchPokemonObjective: Pokemon Species '" + speciesStr + "' not found for quest " + questId);
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
                    Valdora.LOGGER.error("CatchPokemonObjective: Pokemon Type '" + typeStr + "' not found for quest " + questId);
                }
            }
        }

        if (species == null && type == null && !any) {
            Valdora.LOGGER.warn("CatchPokemonObjective: No valid species/type/any configured for quest " + questId);
        }

        if (species != null && type != null) {
            Valdora.LOGGER.warn("CatchPokemonObjective: Both 'species' and 'pkmn_type' provided for quest " + questId + ". Species will take precedence.");
        }
    }

    @Override
    public boolean handleObjectiveUpdate(ActiveQuest activeQuest, ServerPlayerEntity player, Object data) {
        if (!any && species == null && type == null) return false;

        if (!(data instanceof Pokemon caughtPokemon)) return false;

        boolean matched = false;

        if (any) {
            matched = true;
        } else if (species != null) {
            matched = species.equals(caughtPokemon.getSpecies());
        } else if (type != null) {
            ElementalType pPrim = caughtPokemon.getPrimaryType();
            ElementalType pSec = caughtPokemon.getSecondaryType();
            matched = (pPrim != null && pPrim.equals(type)) || (pSec != null && pSec.equals(type));
        }

        if (matched) {
            activeQuest.count++;
            return activeQuest.count >= count;
        }

        return false;
    }
}
