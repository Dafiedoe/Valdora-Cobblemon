package net.valdora.trainers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.Valdora;
import net.valdora.trainers.commands.StartTrainerBattleCommand;
import net.valdora.trainers.events.TrainerBattleEndEvent;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrainerManager {
    private static final String TRAINER_CONFIG_PATH = "config/valdora/trainers/";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ConfigPokemon.class, new ConfigPokemonAdapter())
            .create();

    private static final Map<String, TrainerConfig> TRAINERS = new HashMap<>();
    private static Map<UUID, TrainerConfig> lastBattledTrainer = new HashMap<>();

    public static void register() {
        lastBattledTrainer = new HashMap<>();

        load();

        TrainerBattleEndEvent.register();

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            StartTrainerBattleCommand.register(dispatcher);
        }));
    }

    public static void load() {
        TRAINERS.clear();

        try {
            Path configPath = Paths.get(TRAINER_CONFIG_PATH);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                Valdora.LOGGER.info("Created trainer config directory: " + configPath);
                return;
            }

            Files.walk(configPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try (FileReader reader = new FileReader(path.toFile())) {
                            TrainerConfig trainerConfig = GSON.fromJson(reader, TrainerConfig.class);
                            if (trainerConfig != null && trainerConfig.trainerId != null) {
                                TRAINERS.put(trainerConfig.trainerId, trainerConfig);
                                Valdora.LOGGER.info("Registered trainer: " + trainerConfig.trainerId);
                            } else {
                                Valdora.LOGGER.error("Invalid trainer config in file: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            Valdora.LOGGER.error("Failed to read trainer config from " + path.getFileName() + ": " + e.getMessage());
                        } catch (Exception e) {
                            Valdora.LOGGER.error("Error parsing trainer config from " + path.getFileName() + ": " + e.getMessage());
                        }
                    });

            Valdora.LOGGER.info("Successfully registered " + TRAINERS.size() + " trainers");

        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to access trainer config directory: " + e.getMessage());
        }
    }

    public static Map<String, TrainerConfig> getTrainers() {
        return TRAINERS;
    }

    public static TrainerConfig getTrainerById(String id) {
        if (TRAINERS.containsKey(id)) {
            return TRAINERS.get(id);
        }
        return null;
    }

    public static void playerStartedBattle(ServerPlayerEntity player, TrainerConfig trainer) {
        lastBattledTrainer.put(player.getUuid(), trainer);
    }

    public static TrainerConfig getLastBattle(UUID uuid) {
        if (lastBattledTrainer.containsKey(uuid)) {
            return lastBattledTrainer.get(uuid);
        }
        return null;
    }
}