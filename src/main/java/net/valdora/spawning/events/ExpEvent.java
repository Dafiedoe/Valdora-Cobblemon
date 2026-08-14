package net.valdora.spawning.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.valdora.trainers.TrainerConfig;
import net.valdora.trainers.TrainerManager;

import java.util.UUID;

public class ExpEvent {
    public static void register() {
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.HIGHEST, (Function1<ExperienceGainedEvent.Pre, Unit>) ExpEvent::ExpGained);
    }
    
    private static Unit ExpGained(ExperienceGainedEvent.Pre event) {
        if (event.getPokemon().getOriginalTrainer() == null) return Unit.INSTANCE;
        
        TrainerConfig trainer = TrainerManager.getLastBattle(UUID.fromString(event.getPokemon().getOriginalTrainer()));
        
        if (event.getPokemon().getOriginalTrainer() != null && trainer != null) {
            event.setExperience((int) Math.floor(event.getExperience() * trainer.xpMultiplier));
        }
        return Unit.INSTANCE;
    }
}
