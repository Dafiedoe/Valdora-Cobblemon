package net.valdora.quests.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class CompassHudClient {
    private static Vec3d targetPos = Vec3d.ZERO;
    private static boolean showCompass = false;
    
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(CompassTargetPayload.ID, (payload, context) -> {
            CompassTargetPayload compassPayload = payload;
            targetPos = new Vec3d(compassPayload.x(), compassPayload.y(), compassPayload.z());
            showCompass = compassPayload.showCompass();
        });
    }
    
    public static Vec3d getTargetPos() {
        return targetPos;
    }
    
    public static boolean shouldShowCompass() {
        return showCompass;
    }
    
    public static float getYawToTarget(MinecraftClient client) {
        Vec3d playerPos = client.player.getPos();
        Vec3d target = getTargetPos().relativize(playerPos);
        float targetYawRad = (float) MathHelper.atan2(target.z, target.x) - (float) Math.PI / 2;
        float targetYawDeg = (float) Math.toDegrees(targetYawRad);
        float playerYaw = client.player.getYaw();
        return MathHelper.wrapDegrees(targetYawDeg - playerYaw) + 180;
    }
}
