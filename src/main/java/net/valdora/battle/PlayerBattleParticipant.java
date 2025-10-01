package net.valdora.battle;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import net.minecraft.server.network.ServerPlayerEntity;

public interface PlayerBattleParticipant extends BattleParticipant {
    ServerPlayerEntity getPlayerEntity();

    BattleActor createBattleActor();
}
