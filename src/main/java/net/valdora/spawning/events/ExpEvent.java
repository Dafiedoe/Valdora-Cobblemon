package net.valdora.spawning.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedPreEvent;
import kotlin.Unit;
import net.valdora.Valdora;
import net.valdora.trainers.TrainerConfig;
import net.valdora.trainers.TrainerManager;

import java.util.UUID;

public class ExpEvent {
    public static void register() {
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.HIGHEST, ExpEvent::ExpGained);
    }

    private static Unit ExpGained(ExperienceGainedPreEvent event) {
        if (event.getPokemon().getOriginalTrainer() == null) return Unit.INSTANCE;

        TrainerConfig trainer = TrainerManager.getLastBattle(UUID.fromString(event.getPokemon().getOriginalTrainer()));

        if (event.getPokemon().getOriginalTrainer() != null && trainer != null) {
            event.setExperience((int) Math.floor(event.getExperience() * trainer.xpMultiplier));
        }
        return Unit.INSTANCE;
    }
}
