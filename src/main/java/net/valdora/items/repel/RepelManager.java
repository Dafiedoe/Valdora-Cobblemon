package net.valdora.items.repel;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.valdora.savedata.PlayerSaveDataManager;

import java.util.HashMap;
import java.util.Map;

public class RepelManager {
    private static final Map<String, Vec3d> lastPositions = new HashMap<>();
    
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            AdminRepelCommand.register(dispatcher);
        }));
        
        ServerTickEvents.END_SERVER_TICK.register(RepelManager::onServerTick);
    }
    
    public static void activateRepel(ServerPlayerEntity player, float amount) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        progress.setRepelSteps(amount);
        lastPositions.put(player.getUuid().toString(), player.getPos());
        PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());
    }
    
    public static void toggleAdminRepel(ServerPlayerEntity player) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        progress.toggleAdminRepel();
        PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());
        
        boolean adminRepel = progress.getAdminRepel();
        if (adminRepel) {
            player.sendMessage(Text.literal("Admin repel activated!").formatted(Formatting.GREEN));
        } else {
            player.sendMessage(Text.literal("Admin repel deactivated!").formatted(Formatting.RED));
        }
    }
    
    public static Boolean hasRepel(ServerPlayerEntity player) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        if (progress.getAdminRepel()) return true;
        return progress.getRepelSteps() > 0;
    }
    
    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean changed = false;
            
            PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
            String playerId = player.getUuid().toString();
            
            Vec3d currentPos = player.getPos();
            
            if (progress.getAdminRepel()) {
                lastPositions.put(playerId, currentPos);
                continue;
            }
            
            if (progress.getRepelSteps() <= 0) {
                lastPositions.remove(playerId);
                continue;
            }
            
            Vec3d lastPos = lastPositions.get(playerId);
            
            if (lastPos == null) {
                lastPositions.put(playerId, currentPos);
                continue;
            }
            
            double dx = currentPos.x - lastPos.x;
            double dz = currentPos.z - lastPos.z;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                progress.setRepelSteps(progress.getRepelSteps() - distance);
                
                if (progress.getRepelSteps() <= 0) {
                    player.sendMessage(Text.literal("Your repel has worn off.").formatted(Formatting.RED));
                }
                changed = true;
            }
            
            lastPositions.put(playerId, currentPos);
            
            if (changed) {
                PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());
            }
        }
    }
}
