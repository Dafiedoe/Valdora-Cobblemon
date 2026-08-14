package net.valdora.timespecificevents;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

public class ShinyHour {
    public static LocalTime EVENT_START_TIME = LocalTime.of(8, 0);
    public static long EVENT_DURATION_MINUTES = 60;
    public static long EVENT_INTERVAL_MINUTES = 12;
    public static List<String> possibleBiomes;
    
    private static boolean eventActive = false;
    private static String biome;
    
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ShinyHour::onServerTick);
        
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            ShinyDebugCommand.register(dispatcher);
        }));
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();
            
            long minutesSinceMidnight = ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, currentTime);
            long minutesInInterval = minutesSinceMidnight % EVENT_INTERVAL_MINUTES;
            
            LocalTime startTime = EVENT_START_TIME;
            LocalTime endTime = startTime.plusMinutes(EVENT_DURATION_MINUTES);
            boolean isEventTime = minutesInInterval >= startTime.getHour() * 60 + startTime.getMinute() && minutesInInterval < endTime.getHour() * 60 + endTime.getMinute();
            
            if (isEventTime) {
                String message = isSunday() ? "Welcome! Shiny Sunday Hour is active in " + biome : "Welcome! Shiny Hour is active in " + biome;
                handler.getPlayer().sendMessage(Text.literal(message), false);
            }
        });
    }
    
    private static void onServerTick(MinecraftServer server) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        long minutesSinceMidnight = ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, currentTime);
        long minutesInInterval = minutesSinceMidnight % EVENT_INTERVAL_MINUTES;
        
        LocalTime startTime = EVENT_START_TIME;
        LocalTime endTime = startTime.plusMinutes(EVENT_DURATION_MINUTES);
        boolean isEventTime = minutesInInterval >= startTime.getHour() * 60 + startTime.getMinute() && minutesInInterval < endTime.getHour() * 60 + endTime.getMinute();
        
        if (isEventTime && !eventActive) {
            biome = possibleBiomes.get(new Random().nextInt(possibleBiomes.size()));
            
            eventActive = true;
            if (isSunday()) {
                server.getPlayerManager().broadcast(Text.literal("Shiny sunday hour begin in " + biome), false);
            } else {
                server.getPlayerManager().broadcast(Text.literal("Shiny hour begin in " + biome), false);
            }
        } else if (!isEventTime && eventActive) {
            biome = "";
            
            eventActive = false;
            if (isSunday()) {
                server.getPlayerManager().broadcast(Text.literal("Shiny sunday hour end"), false);
            } else {
                server.getPlayerManager().broadcast(Text.literal("Shiny hour end"), false);
            }
        }
    }
    
    public static boolean isActive() {
        return eventActive;
    }
    
    public static String getBiome() {
        return biome;
    }
    
    public static boolean isSunday() {
        LocalDateTime now = LocalDateTime.now();
        return now.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
