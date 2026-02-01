package net.valdora.quests.objectivetypes;

import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.Valdora;
import net.valdora.quests.ActiveQuest;
import net.valdora.quests.Objective;
import net.valdora.quests.ObjectiveType;
import net.valdora.trainers.TrainerConfig;

public class DefeatTrainersObjective extends Objective {
    public boolean specificTrainer = false;
    public String trainerId;

    public DefeatTrainersObjective(String title, String description, String questId, JsonObject json) {
        super(title, description, ObjectiveType.DEFEAT_TRAINER, questId);

        if (json.has("specific_trainer")) {
            trainerId = json.get("specific_trainer").getAsString();
            specificTrainer = true;

            if (trainerId != null && !trainerId.isEmpty()) {
                Valdora.LOGGER.error("Specific trainer is null or empty in objective: " + title + " in quest " + questId);
            }
        } else {
            specificTrainer = false;
        }
    }

    @Override
    public boolean handleObjectiveUpdate(ActiveQuest activeQuest, ServerPlayerEntity player, Object data) {
        if (specificTrainer) {
            if (trainerId != null && !trainerId.isEmpty()) {
                if (data instanceof TrainerConfig trainerConfig) {
                    if (trainerConfig.trainerId.equals(trainerId)) {
                        activeQuest.count++;
                    }
                }
            }
        } else {
            activeQuest.count++;
        }

        if (activeQuest.count >= count) {
            return true;
        }

        return false;
    }
}
