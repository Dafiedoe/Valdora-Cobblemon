package net.valdora.battle;

import net.valdora.battle.exception.BattleStartException;

import java.util.UUID;

public interface TrainerBattle {
    void start() throws BattleStartException;
    
    UUID getBattleId();
    
    PlayerBattleParticipant getPlayer();
    
    TrainerBattleParticipant getTrainer();
}
