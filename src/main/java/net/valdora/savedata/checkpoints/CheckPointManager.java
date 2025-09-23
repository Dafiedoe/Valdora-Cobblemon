package net.valdora.savedata.checkpoints;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.ParseResults;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.utils.TickScheduler;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CheckPointManager {
    private static final String CHECKPOINT_CONFIG_PATH = "config/valdora/checkpoints/";
    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<String, CheckPoint> CHECKPOINTS = new HashMap<>();

    public static void register() {
        load();

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            RecallCommand.register(dispatcher);
        }));

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getServer().getTicks() % 5 != 0) return;

            checkPlayerCheckPoints(world);
        });
    }

    public static void load() {
        CHECKPOINTS.clear();

        try {
            Path configPath = Paths.get(CHECKPOINT_CONFIG_PATH);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                Valdora.LOGGER.info("Created checkpoint config directory: " + configPath);
                return;
            }

            Files.walk(configPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try (FileReader reader = new FileReader(path.toFile())) {
                            CheckPoint checkPoint = GSON.fromJson(reader, CheckPoint.class);
                            if (checkPoint != null && checkPoint.id != null) {
                                CHECKPOINTS.put(checkPoint.id, checkPoint);
                                Valdora.LOGGER.info("Registered checkpoint: " + checkPoint.id);
                            } else {
                                Valdora.LOGGER.error("Invalid checkpoint config in file: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            Valdora.LOGGER.error("Failed to read checkpoint config from " + path.getFileName() + ": " + e.getMessage());
                        } catch (Exception e) {
                            Valdora.LOGGER.error("Error parsing checkpoint config from " + path.getFileName() + ": " + e.getMessage());
                        }
                    });

            Valdora.LOGGER.info("Successfully registered " + CHECKPOINTS.size() + " checkpoints");

        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to access checkpoint config directory: " + e.getMessage());
        }
    }

    public static Map<String, CheckPoint> getCheckPoints() {
        return CHECKPOINTS;
    }

    public static CheckPoint getCheckPointById(String id) {
        if (CHECKPOINTS.containsKey(id)) {
            return CHECKPOINTS.get(id);
        }
        return null;
    }

    private static void checkPlayerCheckPoints(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            Vec3d currentPos = player.getPos();
            for (Map.Entry<String, CheckPoint> entry : CHECKPOINTS.entrySet()) {
                CheckPoint cp = entry.getValue();

                Box areaBox = new Box(
                        Math.min(cp.pos1X, cp.pos2X), Math.min(cp.pos1Y, cp.pos2Y), Math.min(cp.pos1Z, cp.pos2Z),
                        Math.max(cp.pos1X, cp.pos2X) + 1, Math.max(cp.pos1Y, cp.pos2Y) + 1, Math.max(cp.pos1Z, cp.pos2Z) + 1
                );

                if (areaBox.contains(currentPos)) {
                    onCheckPointEntered(player, cp);
                }
            }
        }
    }

    private static void onCheckPointEntered(ServerPlayerEntity player, CheckPoint cp) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());

        if (progress.getLastCheckPoint() != null && progress.getLastCheckPoint().equals(cp.id)) return;

        progress.setLastCheckPoint(cp.id);

        PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());
    }

    public static void recallPlayerToCheckPoint(ServerPlayerEntity player, boolean healPokemon) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());

        if (healPokemon) {
            TickScheduler.runNextTick(25, () -> Cobblemon.INSTANCE.getStorage().getParty(player).heal());
        }

        if (progress.getLastCheckPoint() == null || (progress.getLastCheckPoint() != null && progress.getLastCheckPoint().isEmpty())) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 3 * 20, 0, false, false));

            MinecraftServer server = player.getServer();
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(Valdora.WORLD_SPAWN_DIMENSION));
            ServerWorld targetWorld = server.getWorld(worldKey);

            TickScheduler.runNextTick(25, () -> {
                player.teleport(targetWorld, Valdora.WORLD_SPAWN_X, Valdora.WORLD_SPAWN_Y, Valdora.WORLD_SPAWN_Z, Valdora.WORLD_SPAWN_YAW, Valdora.WORLD_SPAWN_PITCH);
            });
            return;
        }

        String lastCheckPoint = progress.getLastCheckPoint();
        CheckPoint cp = getCheckPointById(lastCheckPoint);

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 3 * 20, 0, false, false));

        MinecraftServer server = player.getServer();
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(cp.world));
        ServerWorld targetWorld = server.getWorld(worldKey);

        if (targetWorld != null) {
            TickScheduler.runNextTick(25, () -> {
                player.teleport(targetWorld, cp.resetPosX, cp.resetPosY, cp.resetPosZ, cp.resetPosYaw, cp.resetPosPitch);
            });
        } else {
            Valdora.LOGGER.warn("A world with id '" + cp.world + "' does not exist!");
        }
    }
}
