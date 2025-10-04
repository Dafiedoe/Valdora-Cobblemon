package net.valdora.quests;

public class ActiveQuest {
    public String questId;
    public int objectiveIndex;
    public int count;

    public ActiveQuest(String questId, int objectiveIndex, int count) {
        this.questId = questId;
        this.objectiveIndex = objectiveIndex;
        this.count = count;
    }
}
