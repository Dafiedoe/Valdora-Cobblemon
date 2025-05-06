package net.valdora;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.valdora.spawning.SpawnPoolManager;
import net.valdora.commands.DebugValidSpawnsCommand;
import net.valdora.commands.ReloadConfigCommand;
import net.valdora.commands.ReloadSpawnPoolsCommand;
import net.valdora.events.DeletePokemonAfterBattleEvent;
import net.valdora.events.TallGrassWalkEvent;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Valdora implements ModInitializer {
	public static final String MOD_ID = "valdora";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		loadConfig();

		TallGrassWalkEvent.register();
		DeletePokemonAfterBattleEvent.register();

		SpawnPoolManager.load();

		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
			ReloadConfigCommand.register(dispatcher);
			ReloadSpawnPoolsCommand.register(dispatcher);
			DebugValidSpawnsCommand.register(dispatcher);
		}));
	}

	public static void loadConfig() {
		File configFile = new File("config/valdora/config.json");
		if (!configFile.exists()) {
			LOGGER.error("Config file not found. Using default values.");
			return;
		}

		try (FileReader reader = new FileReader(configFile)) {
			Gson gson = new Gson();
			JsonObject config = gson.fromJson(reader, JsonObject.class);

			if (config.has("min_ticks_before_encounter")) {
				TallGrassWalkEvent.MIN_TICKS_BEFORE_ENCOUNTER = config.get("min_ticks_before_encounter").getAsInt();
			}

			if (config.has("encounter_chance_per_tick")) {
				TallGrassWalkEvent.ENCOUNTER_CHANCE_PER_TICK = config.get("encounter_chance_per_tick").getAsDouble();
			}

			LOGGER.info("Config Loaded!");
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("Failed to load config: " + e.getMessage());
		}
	}
}