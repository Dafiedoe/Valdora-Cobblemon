package net.valdora.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.valdora.battle.exception.BattleStartException;
import net.valdora.trainers.TrainerManager;

import java.util.Objects;
import java.util.UUID;

public class AbstractTrainerBattle implements TrainerBattle {
    private final PlayerBattleParticipant player;
    private final TrainerBattleParticipant trainer;
    
    private UUID battleId;
    
    public AbstractTrainerBattle(PlayerBattleParticipant player, TrainerBattleParticipant trainer) {
        this.player = player;
        this.trainer = trainer;
    }
    
    @Override
    public void start() throws BattleStartException {
        Cobblemon.INSTANCE.getStorage().getParty(player.getPlayerEntity()).toGappyList().stream().filter(Objects::nonNull).forEach(Pokemon::recall);
        
        Cobblemon.INSTANCE.getBattleRegistry().startBattle(trainer.getBattleFormat(), new BattleSide(player.createBattleActor()),
                new BattleSide(trainer.createBattleActor(player.getPlayerEntity())), false).ifSuccessful(pokemonBattle -> {
            TrainerManager.playerStartedBattle(player.getPlayerEntity(), trainer.getConfig());
            return Unit.INSTANCE;
        });
    }
    
    @Override
    public UUID getBattleId() {
        return battleId;
    }
    
    @Override
    public PlayerBattleParticipant getPlayer() {
        return player;
    }
    
    @Override
    public TrainerBattleParticipant getTrainer() {
        return trainer;
    }
}
