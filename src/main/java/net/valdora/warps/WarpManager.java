package net.valdora.warps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.valdora.Valdora;
import net.valdora.warps.commands.CreateWarpCommand;
import net.valdora.warps.commands.DeleteWarpCommand;
import net.valdora.warps.commands.WarpCommand;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WarpManager {
    private static final String WARP_SAVE_PATH = "valdora/warps/";
    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<String, Warp> WARPS = new HashMap<>();

    public static void register() {
        load();

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            CreateWarpCommand.register(dispatcher);
            WarpCommand.register(dispatcher);
            DeleteWarpCommand.register(dispatcher);
        }));
    }

    public static void load() {
        WARPS.clear();

        try {
            Path savePath = Paths.get(WARP_SAVE_PATH);
            if (!Files.exists(savePath)) {
                Files.createDirectories(savePath);
                Valdora.LOGGER.info("Created warp save directory: " + savePath);
                return;
            }

            Files.walk(savePath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try (FileReader reader = new FileReader(path.toFile())) {
                            Warp warp = GSON.fromJson(reader, Warp.class);
                            if (warp != null && warp.id != null) {
                                WARPS.put(warp.id, warp);
                                Valdora.LOGGER.info("Registered warp: " + warp.id);
                            } else {
                                Valdora.LOGGER.error("Invalid warp save in file: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            Valdora.LOGGER.error("Failed to read warp save file from " + path.getFileName() + ": " + e.getMessage());
                        } catch (Exception e) {
                            Valdora.LOGGER.error("Error parsing warp save file from " + path.getFileName() + ": " + e.getMessage());
                        }
                    });
            Valdora.LOGGER.info("Successfully registered " + WARPS.size() + " warps");

        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to access warp save directory: " + e.getMessage());
        }
    }

    public static Map<String, Warp> getWarps() {
        return WARPS;
    }

    public static boolean warpExists(String id) {
        return WARPS.containsKey(id);
    }

    public static void createWarp(ServerPlayerEntity player, String id) {
        Warp newWarp = new Warp();
        newWarp.id = id;
        newWarp.world = player.getWorld().getRegistryKey().getValue().toString();
        newWarp.x = Math.floor(player.getPos().x) + 0.5;
        newWarp.y = player.getPos().y;
        newWarp.z = Math.floor(player.getPos().z) + 0.5;
        newWarp.yaw = player.getYaw();
        newWarp.pitch = player.getPitch();

        WARPS.put(newWarp.id, newWarp);
        saveWarp(newWarp);
        player.sendMessage(Text.literal("Warp '" + id + "' created!"));
    }

    private static void saveWarp(Warp warp) {
        Path filePath = Paths.get(WARP_SAVE_PATH, warp.id + ".json");
        try {
            Files.writeString(filePath, GSON.toJson(warp));
            Valdora.LOGGER.info("Saved warp to file: " + filePath);
        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to save warp '" + warp.id + "' to file: " + e.getMessage());
        }
    }

    public static void warpPlayer(ServerPlayerEntity player, String id) {
        if (!warpExists(id)) {
            Valdora.LOGGER.info("Warp with id '" + id + "' does not exist!");
            return;
        }

        Warp warp = WARPS.get(id);
        MinecraftServer server = player.getServer();
        if (server == null) {
            Valdora.LOGGER.error("Server is null, cannot warp player!");
        }

        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(warp.world));
        ServerWorld targetWorld = server.getWorld(worldKey);
        if (targetWorld == null) {
            Valdora.LOGGER.error("Target world ' " + warp.world + "' not found for warp '" + id + "'");
            return;
        }

        player.teleport(targetWorld, warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
        player.sendMessage(Text.literal("Warped to " + id));
    }

    public static void deleteWarp(ServerPlayerEntity player, String id) {
        if (!warpExists(id)) {
            Valdora.LOGGER.info("Warp with id '" + id + "' does not exist!");
            return;
        }

        WARPS.remove(id);
        Path filePath = Paths.get(WARP_SAVE_PATH, id + ".json");
        try {
            Files.deleteIfExists(filePath);
            Valdora.LOGGER.info("Deleted warp file: " + filePath);
        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to delete warp file for '" + id + "': " + e.getMessage());
        }

        player.sendMessage(Text.literal("Deleted warp '" + id + "'"));
    }
}
