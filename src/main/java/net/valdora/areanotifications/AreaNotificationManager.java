package net.valdora.areanotifications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AreaNotificationManager {
    private static final String AREA_NOTIFICATION_CONFIG_PATH = "config/valdora/areanotifications/";
    private static final Gson GSON = new GsonBuilder().create();
    
    private static final Map<String, AreaNotification> AREANOTIFICATIONS = new HashMap<>();
    
    public static final Identifier AREA_NOTIF_CHANNEL = Identifier.of(Valdora.MOD_ID, "send_area_notification");
    
    public static void register() {
        load();
        
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getServer().getTicks() % 5 != 0) return;
            
            checkPlayerAreaNotification(world);
        });
        
        PayloadTypeRegistry.playS2C().register(AreaNotificationPayload.ID, AreaNotificationPayload.CODEC);
    }
    
    public static void load() {
        try {
            Path configPath = Paths.get(AREA_NOTIFICATION_CONFIG_PATH);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                Valdora.LOGGER.info("Created area notification config directory: " + configPath);
                return;
            }
            
            Files.walk(configPath)
                    .filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                        try (FileReader reader = new FileReader(path.toFile())) {
                            AreaNotification area = GSON.fromJson(reader, AreaNotification.class);
                            if (area != null && area.id != null) {
                                AREANOTIFICATIONS.put(area.id, area);
                                Valdora.LOGGER.info("Registered area notification: " + area.id);
                            } else {
                                Valdora.LOGGER.error("Invalid area notification config in file: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            Valdora.LOGGER.error("Failed to read area notification config from " + path.getFileName() + ": " + e.getMessage());
                        } catch (Exception e) {
                            Valdora.LOGGER.error("Error parsing area notification config from " + path.getFileName() + ": " + e.getMessage());
                        }
                    });
            
            Valdora.LOGGER.info("Successfully registered " + AREANOTIFICATIONS.size() + " area notifications");
            
        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to access area notification config directory: " + e.getMessage());
        }
    }
    
    public static Map<String, AreaNotification> getAreaNotifications() {
        return AREANOTIFICATIONS;
    }
    
    public static AreaNotification getAreaNotificationById(String id) {
        if (AREANOTIFICATIONS.containsKey(id)) {
            return AREANOTIFICATIONS.get(id);
        }
        return null;
    }
    
    private static void checkPlayerAreaNotification(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            Vec3d currentPos = player.getPos();
            for (Map.Entry<String, AreaNotification> entry : AREANOTIFICATIONS.entrySet()) {
                AreaNotification area = entry.getValue();
                
                Box areaBox = new Box(Math.min(area.x1, area.x2), Math.min(area.y1, area.y2), Math.min(area.z1, area.z2),
                        Math.max(area.x1, area.x2) + 1, Math.max(area.y1, area.y2) + 1, Math.max(area.z1, area.z2) + 1);
                
                if (areaBox.contains(currentPos)) {
                    onAreaNotificationEntered(player, area);
                }
            }
        }
    }
    
    private static void onAreaNotificationEntered(ServerPlayerEntity player, AreaNotification area) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        
        if (progress.getLastAreaVisited() != null && progress.getLastAreaVisited().equals(area.name)) return;
        
        progress.setLastAreaVisited(area.name);
        
        PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());
        
        triggerNotification(player, "Now entering " + area.name);
    }
    
    private static void triggerNotification(ServerPlayerEntity player, String message) {
        ServerPlayNetworking.send(player, new AreaNotificationPayload(message));
    }
}
