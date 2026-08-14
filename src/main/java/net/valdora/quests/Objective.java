package net.valdora.quests;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public abstract class Objective {
    public String title;
    public String description;
    public ObjectiveType type;
    public List<String> completionCommands;
    public int count;
    public String originQuest;
    
    public Objective(String title, String description, ObjectiveType type, String questId) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.originQuest = questId;
    }
    
    public void setCompletionCommands(List<String> completionCommands) {
        this.completionCommands = completionCommands;
    }
    
    public abstract boolean handleObjectiveUpdate(ActiveQuest activeQuest, ServerPlayerEntity player, Object data);
}
