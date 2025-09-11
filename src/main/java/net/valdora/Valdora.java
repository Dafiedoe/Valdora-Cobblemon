package net.valdora;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.general.*;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.savedata.flaggedbarrier.PlayerFlagsS2CPayload;
import net.valdora.trainers.TrainerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.valdora.spawning.ModSpawning;
import net.valdora.spawning.events.TallGrassWalkEvent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Main mod class for Valdora.
 */
public class Valdora implements ModInitializer {
	public static final String MOD_ID = "valdora";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/*
	 * World spawn settings (configurable).
	 * - SPAWN_DIMENSION is a registry id string (e.g. "minecraft:overworld" or a modded dimension id).
	 * - SPAWN_X/Y/Z are the coordinates to teleport to.
	 * - SPAWN_YAW/PITCH are the orientation to set on teleport.
	 */
	public static String WORLD_SPAWN_DIMENSION = "minecraft:overworld";
	public static double WORLD_SPAWN_X = 0.0;
	public static double WORLD_SPAWN_Y = 64.0;
	public static double WORLD_SPAWN_Z = 0.0;
	public static float WORLD_SPAWN_YAW = 0.0f;
	public static float WORLD_SPAWN_PITCH = 0.0f;

	@Override
	public void onInitialize() {
		LOGGER.info("[Valdora] Initializing mod");

		loadConfig();

		new PlayerSaveDataManager();
		PlayerSaveDataManager.INSTANCE.register();
		ModEntities.register();
		ModSpawning.registerSpawning();
		ModComponents.initialize();
		ModBlocks.register();
		ModBlockEntities.register();
		TrainerManager.register();

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		PayloadTypeRegistry.playS2C().register(PlayerFlagsS2CPayload.PAYLOAD_ID, PlayerFlagsS2CPayload.CODEC);

		LOGGER.info("[Valdora] Initialization complete");
	}

	private void onServerTick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			SurfManager.tick(player);
		}
	}

	/**
	 * Reads config/valdora/config.json (if exists) and applies settings.
	 *
	 * Expected JSON:
	 * {
	 *   "min_ticks_before_encounter": 20,
	 *   "encounter_chance_per_tick": 0.001,
	 *   "world_spawn": {
	 *     "dimension": "minecraft:overworld",
	 *     "x": 0.0,
	 *     "y": 64.0,
	 *     "z": 0.0,
	 *     "yaw": 0.0,
	 *     "pitch": 0.0
	 *   }
	 * }
	 */
	public static void loadConfig() {
		File configFile = new File("config/valdora/config.json");
		if (!configFile.exists()) {
			LOGGER.warn("[Valdora] No config file found; using defaults");
			LOGGER.info("[Valdora] Default world spawn: {} @ ({}, {}, {}) yaw/pitch: {}/{}",
					WORLD_SPAWN_DIMENSION, WORLD_SPAWN_X, WORLD_SPAWN_Y, WORLD_SPAWN_Z, WORLD_SPAWN_YAW, WORLD_SPAWN_PITCH);
			return;
		}

		try (FileReader reader = new FileReader(configFile)) {
			JsonObject cfg = new Gson().fromJson(reader, JsonObject.class);

			if (cfg.has("min_ticks_before_encounter")) {
				TallGrassWalkEvent.MIN_TICKS_BEFORE_ENCOUNTER =
						cfg.get("min_ticks_before_encounter").getAsInt();
			}
			if (cfg.has("encounter_chance_per_tick")) {
				TallGrassWalkEvent.ENCOUNTER_CHANCE_PER_TICK =
						cfg.get("encounter_chance_per_tick").getAsDouble();
			}

			if (cfg.has("world_spawn") && cfg.get("world_spawn").isJsonObject()) {
				JsonObject spawn = cfg.getAsJsonObject("world_spawn");
				try {
					if (spawn.has("dimension") && spawn.get("dimension").isJsonPrimitive()) {
						WORLD_SPAWN_DIMENSION = spawn.get("dimension").getAsString();
					}
					if (spawn.has("x") && spawn.get("x").isJsonPrimitive()) {
						WORLD_SPAWN_X = spawn.get("x").getAsDouble();
					}
					if (spawn.has("y") && spawn.get("y").isJsonPrimitive()) {
						WORLD_SPAWN_Y = spawn.get("y").getAsDouble();
					}
					if (spawn.has("z") && spawn.get("z").isJsonPrimitive()) {
						WORLD_SPAWN_Z = spawn.get("z").getAsDouble();
					}
					if (spawn.has("yaw") && spawn.get("yaw").isJsonPrimitive()) {
						WORLD_SPAWN_YAW = spawn.get("yaw").getAsFloat();
					}
					if (spawn.has("pitch") && spawn.get("pitch").isJsonPrimitive()) {
						WORLD_SPAWN_PITCH = spawn.get("pitch").getAsFloat();
					}
					LOGGER.info("[Valdora] Loaded world spawn from config: {} @ ({}, {}, {}) yaw/pitch: {}/{}",
							WORLD_SPAWN_DIMENSION, WORLD_SPAWN_X, WORLD_SPAWN_Y, WORLD_SPAWN_Z, WORLD_SPAWN_YAW, WORLD_SPAWN_PITCH);
				} catch (Exception e) {
					LOGGER.warn("[Valdora] Invalid world_spawn entry in config.json; using defaults", e);
				}
			} else {
				LOGGER.info("[Valdora] Using default world spawn: {} @ ({}, {}, {}) yaw/pitch: {}/{}",
						WORLD_SPAWN_DIMENSION, WORLD_SPAWN_X, WORLD_SPAWN_Y, WORLD_SPAWN_Z, WORLD_SPAWN_YAW, WORLD_SPAWN_PITCH);
			}

			LOGGER.info("[Valdora] Config loaded successfully");
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("[Valdora] Failed to load config", e);
		}
	}
}
