package net.valdora;

import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.valdora.areanotifications.AreaNotificationManager;
import net.valdora.battle.Generation5AI;
import net.valdora.events.PokemonEvolutionEvent;
import net.valdora.general.*;
import net.valdora.items.repel.RepelItem;
import net.valdora.items.repel.RepelManager;
import net.valdora.quests.ActiveQuest;
import net.valdora.quests.Quest;
import net.valdora.quests.QuestManager;
import net.valdora.quests.objectivetypes.ReachLocationObjective;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.savedata.checkpoints.CheckPointManager;
import net.valdora.shops.ShopManager;
import net.valdora.timespecificevents.ShinyHour;
import net.valdora.trainers.TrainerManager;
import net.valdora.utils.PokemonUtils;
import net.valdora.warps.WarpManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.valdora.spawning.ModSpawning;
import net.valdora.spawning.events.TallGrassWalkEvent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;

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

	public static int BASE_SHINY_CHANCE = 8192;
	public static int SHINY_TIME_MULTIPLIER = 8;
	public static int SHINY_SUNDAY_MULTIPLIER = 2;

	private static boolean reloadQuestHud = false;

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
		ModItems.register();
		ModItemGroups.register();
		WarpManager.register();
		TrainerManager.register();
		QuestManager.register();
		CheckPointManager.register();
		ShopManager.register();
		ShinyHour.register();
		AreaNotificationManager.register();
		RepelManager.register();

		Generation5AI.initialiseTypeChart();

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		PokemonEvolutionEvent.register();

		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
			ReloadModCommand.register(dispatcher);
		}));

		LOGGER.info("[Valdora] Initialization complete");
	}

	private void onServerTick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			SurfManager.tick(player);
			if (PokemonUtils.hasPokemon(player) && !PokemonUtils.hasPokemonAvailable(player) && !CheckPointManager.isPlayerRecalling(player)) {
				CheckPointManager.recallPlayerToCheckPoint(player, true);
			}
			if (reloadQuestHud) {
				reloadQuestHudForPlayer(player);
			}
		}
		reloadQuestHud = false;
	}

	/**
	 * Reads config/valdora/config.json (if exists) and applies settings.
	 *
	 * Expected JSON structure:
	 * {
	 *   "min_ticks_before_encounter": 20,              // Minimum ticks before an encounter can occur
	 *   "encounter_chance_per_tick": 0.001,           // Chance of an encounter per tick
	 *   "world_spawn": {                              // World spawn coordinates and orientation
	 *     "dimension": "minecraft:overworld",         // Spawn dimension
	 *     "x": 0.0,                                   // X coordinate
	 *     "y": 64.0,                                  // Y coordinate
	 *     "z": 0.0,                                   // Z coordinate
	 *     "yaw": 0.0,                                 // Yaw rotation
	 *     "pitch": 0.0                               // Pitch rotation
	 *   },
	 *   "shiny_biomes": [                             // List of biomes where shiny encounters can occur
	 *     "plains",
	 *     "forest"
	 *   ],
	 *   "base_shiny_chance": 8192,                    // Base chance for shiny encounter (1 in N)
	 *   "shiny_time_multiplier": 8,                   // Multiplier for shiny chance during events
	 *   "shiny_sunday_multiplier": 2,                 // Multiplier for shiny chance on Sundays
	 *   "shiny_time_start_time_h": 14,                // Hour for shiny event start time (24-hour format)
	 *   "shiny_time_start_time_m": 0,                 // Minute for shiny event start time
	 *   "shiny_time_duration": 60,                    // Duration of shiny event in minutes
	 *   "shiny_time_interval": 720                    // Interval between shiny events in minutes
	 *   "default_repel_distance": 100                 // Distance of default repel
	 *   "super_repel_distance": 200                   // Distance of super repel
	 *   "max_repel_distance": 250                     // Distance of max repel
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

			if (cfg.has("shiny_biomes") && cfg.get("shiny_biomes").isJsonArray()) {
				JsonArray biomes = cfg.get("shiny_biomes").getAsJsonArray();

				if (biomes.isEmpty()) {
					LOGGER.warn("[Valdora] Shiny biomes array is empty");
				}
				ShinyHour.possibleBiomes = new ArrayList<>();

				for (JsonElement biome : biomes) {
					ShinyHour.possibleBiomes.add(biome.toString().replaceAll("^\"|\"$", ""));
				}
			}

			if (cfg.has("base_shiny_chance")) {
				BASE_SHINY_CHANCE = cfg.get("base_shiny_chance").getAsInt();
			}

			if (cfg.has("shiny_time_multiplier")) {
				SHINY_TIME_MULTIPLIER = cfg.get("shiny_time_multiplier").getAsInt();
			}

			if (cfg.has("shiny_sunday_multiplier")) {
				SHINY_SUNDAY_MULTIPLIER = cfg.get("shiny_sunday_multiplier").getAsInt();
			}

			if (cfg.has("shiny_time_start_time_h") && cfg.has("shiny_time_start_time_m")) {
				int h = cfg.get("shiny_time_start_time_h").getAsInt();
				int m = cfg.get("shiny_time_start_time_m").getAsInt();

				ShinyHour.EVENT_START_TIME = LocalTime.of(h, m);
			}

			if (cfg.has("shiny_time_duration")) {
				ShinyHour.EVENT_DURATION_MINUTES = cfg.get("shiny_time_duration").getAsInt();
			}

			if (cfg.has("shiny_time_interval")) {
				ShinyHour.EVENT_INTERVAL_MINUTES = cfg.get("shiny_time_interval").getAsInt();
			}

			if (cfg.has("default_repel_distance")) {
				RepelItem.DEFAULT_STEPS = cfg.get("default_repel_distance").getAsInt();
			}

			if (cfg.has("super_repel_distance")) {
				RepelItem.SUPER_STEPS = cfg.get("super_repel_distance").getAsInt();
			}

			if (cfg.has("max_repel_distance")) {
				RepelItem.MAX_STEPS = cfg.get("max_repel_distance").getAsInt();
			}

			LOGGER.info("[Valdora] Config loaded successfully");
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("[Valdora] Failed to load config", e);
		}
	}

	public static void reloadPlayerQuestHud() {
		reloadQuestHud = true;
	}

	private static void reloadQuestHudForPlayer(ServerPlayerEntity player) {
		PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
		if (progress != null) {
			Quest quest = QuestManager.getQuestById(progress.getTrackingQuest());
			ActiveQuest activeQuest = progress.getActiveQuestById(progress.getTrackingQuest());
			if (quest != null && activeQuest != null) {
				QuestManager.sendQuestHudUpdate(player, quest, activeQuest.objectiveIndex, activeQuest.count, quest.getObjectiveByIndex(activeQuest.objectiveIndex).count);
				if (quest.getObjectiveByIndex(activeQuest.objectiveIndex) instanceof ReachLocationObjective reachLocationObjective) {
					QuestManager.sendCompassUpdate(player, new Vec3d(reachLocationObjective.x, reachLocationObjective.y, reachLocationObjective.z), reachLocationObjective.showCompass);
				} else {
					QuestManager.sendCompassUpdate(player, player.getPos(), false);
				}
			}
		}
	}
}
