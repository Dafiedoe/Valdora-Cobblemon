package net.valdora.battle;

import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.ai.RandomBattleAI;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;

import java.util.Objects;

public class BattleAIFactory implements SimpleFactory<BattleAI> {
    private final String battleFormat;
    private final String battleAI;
    
    public BattleAIFactory(String battleFormat, String battleAI) {
        this.battleFormat = battleFormat;
        this.battleAI = battleAI;
    }
    
    @Override
    public BattleAI create() {
        if (Objects.equals(battleFormat, "single")) {
            return new SingleBattleAIFactory().create();
        } else {
            return new NonSingleBattleAIFactory().create();
        }
    }
    
    private class SingleBattleAIFactory implements SimpleFactory<BattleAI> {
        @Override
        public BattleAI create() {
            return switch (battleAI) {
                case "random" -> new RandomBattleAI();
                case "generation5" -> new Generation5AI();
                case "strong0" -> new StrongBattleAI(0);
                case "strong1" -> new StrongBattleAI(1);
                case "strong2" -> new StrongBattleAI(2);
                case "strong3" -> new StrongBattleAI(3);
                case "strong4" -> new StrongBattleAI(4);
                case "strong5" -> new StrongBattleAI(5);
                default -> new Generation5AI();
            };
        }
    }
    
    private class NonSingleBattleAIFactory implements SimpleFactory<BattleAI> {
        @Override
        public BattleAI create() {
            return switch (battleAI) {
                case "random" -> new RandomBattleAI();
                case "generation5" -> new Generation5AI();
                default -> new Generation5AI();
            };
        }
    }
}
