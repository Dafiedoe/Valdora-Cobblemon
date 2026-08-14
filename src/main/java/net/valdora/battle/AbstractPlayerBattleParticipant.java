package net.valdora.battle;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AbstractPlayerBattleParticipant implements PlayerBattleParticipant {
    private final ServerPlayerEntity player;
    private final PartyStore party;
    
    public AbstractPlayerBattleParticipant(ServerPlayerEntity player, PartyStore party) {
        this.player = player;
        this.party = party;
    }
    
    public UUID getUuid() {
        return player.getUuid();
    }
    
    @Override
    public PartyStore getParty() {
        return party;
    }
    
    public ServerPlayerEntity getPlayerEntity() {
        return player;
    }
    
    @Override
    public String getName() {
        return player.getGameProfile().getName();
    }
    
    @Override
    public BattleActor createBattleActor() {
        return new PlayerBattleActor(getUuid(), getBattleTeam());
    }
    
    private List<BattlePokemon> getBattleTeam() {
        return party.toBattleTeam(false, false, getLeadingPokemon());
    }
    
    private UUID getLeadingPokemon() {
        return party.toGappyList().stream().filter(Objects::nonNull).filter(pokemon -> !pokemon.isFainted()).toList().get(0).getUuid();
    }
}
