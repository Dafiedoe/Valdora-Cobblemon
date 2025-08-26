package net.valdora.utils;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class PokemonTeamBattleActor extends AIBattleActor {
    private final String name;
    private final String id;
    private final String npcUuid;

    public PokemonTeamBattleActor(String name, String id, String npcUuid, @NotNull UUID uuid, @NotNull List<? extends BattlePokemon> pokemonList, @NotNull BattleAI battleAI) {
        super(uuid, pokemonList, battleAI);
        this.name = name;
        this.id = id;
        this.npcUuid = npcUuid;
    }

    @NotNull
    @Override
    public ActorType getType() {
        return ActorType.NPC;
    }

    @NotNull
    @Override
    public MutableText getName() {
        return Text.literal(this.name);
    }

    @NotNull
    @Override
    public MutableText nameOwned(@NotNull String pokemonName) {
        return Text.literal(this.name + "'s " + pokemonName);
    }

    public String getId() {
        return id;
    }

    public String getNpcUuid() {
        return npcUuid;
    }
}
