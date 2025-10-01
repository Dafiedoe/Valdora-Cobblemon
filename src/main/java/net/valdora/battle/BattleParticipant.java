package net.valdora.battle;

import com.cobblemon.mod.common.api.storage.party.PartyStore;

import java.util.UUID;

public interface BattleParticipant {
    String getName();

    UUID getUuid();

    PartyStore getParty();
}
