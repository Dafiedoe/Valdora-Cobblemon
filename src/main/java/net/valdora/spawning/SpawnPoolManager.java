package net.valdora.spawning;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.valdora.Valdora;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SpawnPoolManager {
    private static final String SPAWN_POOL_FILE_PATH = "config/valdora/spawn_pools.json";
    private static final String BIOME_SETTINGS_FILE_PATH = "config/valdora/biome_spawn_settings.json";
    private static final Gson GSON = new Gson();
    
    private static Map<String, List<SpawnEntry>> spawnPools;
    private static Map<String, BiomeSpawnSettings> biomeSettings;
    
    public static void load() {
        Path spawnPoolPath = Path.of(SPAWN_POOL_FILE_PATH);
        Path biomeSettingsPath = Path.of(BIOME_SETTINGS_FILE_PATH);
        
        try {
            Files.createDirectories(spawnPoolPath.getParent());
            
            if (Files.notExists(spawnPoolPath)) {
                Files.createFile(spawnPoolPath);
                Files.writeString(spawnPoolPath, "{}");
            }
            
            if (Files.notExists(biomeSettingsPath)) {
                Files.createFile(biomeSettingsPath);
                Files.writeString(biomeSettingsPath, "{}");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        try (Reader reader = new InputStreamReader(Files.newInputStream(spawnPoolPath))) {
            Type type = new TypeToken<Map<String, List<SpawnEntry>>>() { }.getType();
            spawnPools = GSON.fromJson(reader, type);
            if (spawnPools == null) spawnPools = new HashMap<>();
            Valdora.LOGGER.info("Pokemon spawns loaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            spawnPools = new HashMap<>();
        }
        
        try (Reader reader = new InputStreamReader(Files.newInputStream(biomeSettingsPath))) {
            Type type = new TypeToken<Map<String, BiomeSpawnSettings>>() { }.getType();
            biomeSettings = GSON.fromJson(reader, type);
            if (biomeSettings == null) biomeSettings = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            biomeSettings = new HashMap<>();
        }
    }
    
    public static List<SpawnEntry> getSpawnsForBiome(String biome) {
        return spawnPools.getOrDefault(biome, Collections.emptyList());
    }
    
    public static List<SpawnEntry> getValidSpawnsForPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        String biomeId = biomeEntry.getKey().map(k -> k.getValue().getPath()).orElse("unknown");
        
        List<SpawnEntry> entries = getSpawnsForBiome(biomeId);
        
        long timeOfDay = world.getTimeOfDay() % 24000;
        String time = timeOfDay >= 0 && timeOfDay < 13000 ? "Day" : "Night";
        
        String weather;
        if (world.isThundering()) {
            weather = "Thunder";
        } else if (world.isRaining()) {
            weather = "Rain";
        } else {
            weather = "Clear";
        }
        
        return entries.stream().filter(entry -> entry.time.equalsIgnoreCase("Any") || entry.time.equalsIgnoreCase(time))
                .filter(entry -> entry.weather.equalsIgnoreCase("Any") || entry.weather.equalsIgnoreCase(weather)).collect(Collectors.toList());
    }
    
    public static BiomeSpawnSettings getSettingsForBiome(String biome) {
        return biomeSettings.getOrDefault(biome, null);
    }
}
