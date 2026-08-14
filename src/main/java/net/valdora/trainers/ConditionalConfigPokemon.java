package net.valdora.trainers;

import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.savedata.PlayerSaveDataManager;

public class ConditionalConfigPokemon extends ConfigPokemon {
    public String requiredFlag;
    public String requiredValue;
    
    public ConditionalConfigPokemon() {
        super();
    }
    
    public boolean isAllowedForPlayer(ServerPlayerEntity player) {
        if (requiredFlag == null || requiredFlag.isEmpty()) {
            return true;
        }
        
        PlayerSaveDataManager.PlayerStoryProgress progress =
                PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        
        return progress.checkFlag(requiredFlag.toLowerCase(), requiredValue.toLowerCase());
    }
}
