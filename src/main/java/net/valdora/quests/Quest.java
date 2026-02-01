package net.valdora.quests;

import java.util.List;

public class Quest {
    public String id;
    public String title;
    public String description;

    public List<Objective> objectives;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public List<Objective> getObjectives() { return objectives; }

    public Objective getObjectiveByIndex(int index) {
        if (index < objectives.size()) {
            return objectives.get(index);
        }
        return null;
    }
}
