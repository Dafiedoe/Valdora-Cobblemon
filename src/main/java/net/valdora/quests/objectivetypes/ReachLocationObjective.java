package net.valdora.quests.objectivetypes;

import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.valdora.quests.ActiveQuest;
import net.valdora.quests.Objective;
import net.valdora.quests.ObjectiveType;

public class ReachLocationObjective extends Objective {
    public double x, y, z;
    public double radius;
    public boolean showCompass;
    
    public ReachLocationObjective(String title, String description, String questId, JsonObject json) {
        super(title, description, ObjectiveType.REACH_LOCATION, questId);
        
        x = json.get("x").getAsDouble();
        y = json.get("y").getAsDouble();
        z = json.get("z").getAsDouble();
        radius = json.get("radius").getAsDouble();
        showCompass = json.get("show_compass").getAsBoolean();
    }
    
    @Override
    public boolean handleObjectiveUpdate(ActiveQuest activeQuest, ServerPlayerEntity player, Object data) {
        if (!(data instanceof Vec3d playerPos)) {
            return false;
        }
        
        Vec3d targetPos = new Vec3d(x, y, z);
        
        return playerPos.distanceTo(targetPos) <= radius;
    }
}
