package net.valdora.quests.objectivetypes;

import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.quests.ActiveQuest;
import net.valdora.quests.Objective;
import net.valdora.quests.ObjectiveType;

public class CommandDrivenObjective extends Objective {
    public CommandDrivenObjective(String title, String description, String questId, JsonObject json) {
        super(title, description, ObjectiveType.COMMAND_DRIVEN, questId);
    }

    @Override
    public boolean handleObjectiveUpdate(ActiveQuest activeQuest, ServerPlayerEntity player, Object data) {
        if (!(data instanceof String dataString)) {
            return false;
        }

        if (dataString.equals(originQuest)) {
            activeQuest.count++;
        }

        if (activeQuest.count >= count) {
            return true;
        }

        return false;
    }
}
