package net.valdora.trainers;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

import java.util.*;

public class ConfigPokemon {
    private static final Map<String, Set<String>> FORM_ASPECTS = Map.ofEntries(
            Map.entry("Normal", Set.of()),
            Map.entry("Alola", Set.of("alolan")),
            Map.entry("Alola Bias", Set.of("region-bias-alola")),
            Map.entry("Galar", Set.of("galarian")),
            Map.entry("Galar Bias", Set.of("region-bias-galar")),
            Map.entry("Hisui", Set.of("hisuian")),
            Map.entry("Hisui Bias", Set.of("region-bias-hisui")),
            Map.entry("Paldea", Set.of("paldean")),
            Map.entry("Paldea Bias", Set.of("region-bias-paldea")),
            Map.entry("Paldea Aqua", Set.of("aqua-breed")),
            Map.entry("Paldea Blaze", Set.of("blaze-breed")),
            Map.entry("Paldea Combat", Set.of("combat-breed")),
            Map.entry("Therian", Set.of("therian")),
            Map.entry("Zen", Set.of("zen_mode")),
            Map.entry("Galar Zen", Set.of("galarian", "zen_mode")),
            Map.entry("Heat", Set.of("heat-appliance")),
            Map.entry("Wash", Set.of("wash-appliance")),
            Map.entry("Frost", Set.of("frost-appliance")),
            Map.entry("Fan", Set.of("fan-appliance")),
            Map.entry("Mow", Set.of("mow-appliance"))
    );

    public String species;
    public String form;
    public int level;
    public String nature;
    public String gender;
    public boolean isShiny;
    public String[] moves;
    public String ability;
    public String heldItem;
    @SerializedName("ivs")
    public IVsConfig ivs;
    @SerializedName("evs")
    public EVsConfig evs;

    public ConfigPokemon() {
        ivs = new IVsConfig();
        evs = new EVsConfig();
    }

    public static class IVsConfig {
        public int hp;
        public int attack;
        public int defense;
        @SerializedName("specialAttack")
        public int specialAttack;
        @SerializedName("specialDefense")
        public int specialDefense;
        public int speed;
    }

    public static class EVsConfig {
        public int hp;
        public int attack;
        public int defense;
        @SerializedName("specialAttack")
        public int specialAttack;
        @SerializedName("specialDefense")
        public int specialDefense;
        public int speed;
    }

    public Pokemon build() {
        // Create pokemon with species
        Pokemon pokemon = new Pokemon();
        Species pkmnSpecies = PokemonSpecies.INSTANCE.getByName(species);
        if (pkmnSpecies == null) {
            Valdora.LOGGER.error("No Pokemon species named '" + species + "' exists");
            return null;
        }
        pokemon.setSpecies(pkmnSpecies);

        // Set form using aspects
        if (form != null && !form.isEmpty()) {
            List<FormData> availableForms = pokemon.getSpecies().getForms();

            Optional<FormData> matchedForm = availableForms.stream()
                    .filter(formData -> formData.getName().equalsIgnoreCase(form))
                    .findFirst();

            if (matchedForm.isPresent()) {
                String formName = matchedForm.get().getName();
                Set<String> aspects = FORM_ASPECTS.getOrDefault(formName, Set.of());
                pokemon.setForcedAspects(new HashSet<>(aspects));
            } else {
                Valdora.LOGGER.warn("No form named '" + form + "' found for " + species + ". Keeping default form.");
            }
        }

        // Set level
        if (level < 1 || level > 100) {
            Valdora.LOGGER.error("Pokemon level cannot be lower than 1 or higher than 100. Level given: " + level);
            return null;
        }
        pokemon.setLevel(level);

        // Set nature
        Nature pkmnNature = Natures.INSTANCE.getNature(nature);
        if (pkmnNature == null) {
            Valdora.LOGGER.error("No Pokemon nature named '" + nature + "' exists");
            return null;
        }
        pokemon.setNature(pkmnNature);

        // Set gender
        try {
            Gender pkmnGender = Gender.valueOf(gender.toUpperCase());
            pokemon.setGender(pkmnGender);
        } catch (IllegalArgumentException e) {
            Valdora.LOGGER.error("No Pokemon gender named '" + gender + "' exists. Genders available: 'MALE', 'FEMALE', 'GENDERLESS'");
            return null;
        }

        // Set shiny
        pokemon.setShiny(isShiny);

        // Set moveset
        MoveSet moveset = pokemon.getMoveSet();
        moveset.clear();
        if (moves == null || moves.length == 0) {
            Valdora.LOGGER.error("A pokemon must have at least 1 move");
            return null;
        }
        if (moves.length > 4) {
            Valdora.LOGGER.error("A pokemon can have up to 4 moves");
            return null;
        }
        for (int i = 0; i < moves.length; i++) {
            MoveTemplate moveTemplate = Moves.INSTANCE.getByName(moves[i]);
            if (moveTemplate == null) {
                Valdora.LOGGER.error("No move named '" + moves[i] + "' exists");
                return null;
            }
            moveset.setMove(i, moveTemplate.create());
        }

        // Set ability
        AbilityTemplate pkmnAbility = Abilities.INSTANCE.get(ability);
        if (pkmnAbility == null) {
            Valdora.LOGGER.error("No ability with the name '" + ability + "' exists");
            return null;
        }
        pokemon.updateAbility(pkmnAbility.create(true, Priority.HIGH));

        // Set held item
        if (heldItem != null && !heldItem.equalsIgnoreCase("none")) {
            ItemStack pkmnHeldItem = new ItemStack(Registries.ITEM.get(Identifier.of(heldItem)));
            if (pkmnHeldItem.isEmpty()) {
                Valdora.LOGGER.error("No held item named '" + heldItem + "' exists");
                return null;
            }
            pokemon.setHeldItem$common(pkmnHeldItem);
        }

        // Set IVs
        if (ivs != null) {
            IVs pokemonIVs = pokemon.getIvs();
            pokemonIVs.set(Stats.HP, ivs.hp);
            pokemonIVs.set(Stats.ATTACK, ivs.attack);
            pokemonIVs.set(Stats.DEFENCE, ivs.defense);
            pokemonIVs.set(Stats.SPECIAL_ATTACK, ivs.specialAttack);
            pokemonIVs.set(Stats.SPECIAL_DEFENCE, ivs.specialDefense);
            pokemonIVs.set(Stats.SPEED, ivs.speed);
        }

        // Set EVs
        if (evs != null) {
            EVs pokemonEVs = pokemon.getEvs();
            pokemonEVs.set(Stats.HP, evs.hp);
            pokemonEVs.set(Stats.ATTACK, evs.attack);
            pokemonEVs.set(Stats.DEFENCE, evs.defense);
            pokemonEVs.set(Stats.SPECIAL_ATTACK, evs.specialAttack);
            pokemonEVs.set(Stats.SPECIAL_DEFENCE, evs.specialDefense);
            pokemonEVs.set(Stats.SPEED, evs.speed);
        }

        PokemonProperties.Companion.parse("uncatchable=yes").apply(pokemon);
        return pokemon;
    }
}