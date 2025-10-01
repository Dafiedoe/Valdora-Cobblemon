package net.valdora.battle;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.trainers.TrainerConfig;
import net.valdora.utils.PokemonTeamBattleActor;

import java.util.List;

public interface TrainerBattleParticipant extends BattleParticipant {
    String getId();

    BattleAI getBattleAI();

    BattleFormat getBattleFormat();

    List<BattlePokemon> getBattleTeam(ServerPlayerEntity player);

    PokemonTeamBattleActor createBattleActor(ServerPlayerEntity player);

    LivingEntity getEntity(ServerPlayerEntity player);

    TrainerConfig getConfig();
}
