package net.valdora;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.cobblemon.mod.common.pokemon.helditem.BaseCobblemonHeldItemManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.valdora.general.SurfManager;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.trainers.TrainerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.valdora.general.ModEntities;
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

	@Override
	public void onInitialize() {
		LOGGER.info("[Valdora] Initializing mod");

		loadConfig();

		new PlayerSaveDataManager();
		PlayerSaveDataManager.INSTANCE.register();
		ModEntities.register();
		ModSpawning.registerSpawning();
		TrainerManager.register();

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		LOGGER.info("[Valdora] Initialization complete");
	}

	private void onServerTick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			SurfManager.tick(player);
		}
	}

	/**
	 * Reads config/valdora/config.json (if exists) and applies settings.
	 */
	public static void loadConfig() {
		File configFile = new File("config/valdora/config.json");
		if (!configFile.exists()) {
			LOGGER.warn("[Valdora] No config file found; using defaults");
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

			LOGGER.info("[Valdora] Config loaded successfully");
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("[Valdora] Failed to load config", e);
		}
	}
}