package net.valdora.quests.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class QuestHudClient {
    private static String currentQuestTitle = "";
    private static String currentObjectiveTitle = "";
    private static int currentCount = 0;
    private static int requiredCount = 0;
    
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(QuestHudPayload.ID, (payload, context) -> {
            QuestHudClient.updateQuestHud(payload.questTitle(), payload.objectiveTitle(), payload.curCount(), payload.reqCount());
        });
    }
    
    public static void updateQuestHud(String questTitle, String objectiveTitle, int curCount, int reqCount) {
        currentQuestTitle = questTitle;
        currentObjectiveTitle = objectiveTitle;
        currentCount = curCount;
        requiredCount = reqCount;
    }
    
    public static boolean hasActiveQuest() {
        return !currentQuestTitle.isEmpty();
    }
    
    public static List<Text> getQuestDisplayLines() {
        if (!hasActiveQuest()) {
            return List.of();
        }
        List<Text> returnList = new ArrayList<>();
        returnList.add(Text.literal("Quest: " + currentQuestTitle));
        returnList.add(Text.literal("Objective: " + currentObjectiveTitle));
        if (requiredCount > 1) {
            returnList.add(Text.literal(currentCount + " / " + requiredCount));
        }
        return returnList;
    }
}
