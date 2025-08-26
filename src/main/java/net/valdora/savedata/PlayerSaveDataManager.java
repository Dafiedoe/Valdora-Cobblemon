package net.valdora.savedata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSaveDataManager {
    public static PlayerSaveDataManager INSTANCE;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private MinecraftServer server;
    private final Map<UUID, PlayerStoryProgress> playerProgress = new HashMap<>();

    public PlayerSaveDataManager() {
        if (INSTANCE == null) {
            INSTANCE = this;
        }
    }

    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> this.server = server);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            saveProgress(uuid);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("valdora")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.literal("checkflag")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("flag", StringArgumentType.string())
                                            .then(CommandManager.argument("fromUUID", StringArgumentType.string())
                                                    .executes(context -> {
                                                        ServerCommandSource source = context.getSource();
                                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                        String flag = StringArgumentType.getString(context, "flag");
                                                        String fromUUID = StringArgumentType.getString(context, "fromUUID");
                                                        PlayerStoryProgress progress = getProgress(player.getUuid());
                                                        String command;
                                                        if (progress.getFlags().containsKey(flag)) {
                                                            command = "easy_npc dialog open " + fromUUID + " " + player.getName().getString() + " " + flag + "_" + progress.getFlags().get(flag);
                                                        } else {
                                                            command = "easy_npc dialog open " + fromUUID + " " + player.getName().getString() + " noflag_" + flag;
                                                        }
                                                        ParseResults<ServerCommandSource> parseResults = source.getServer().getCommandManager().getDispatcher().parse(command, source);
                                                        source.getServer().getCommandManager().execute(parseResults, command);
                                                        return 1;
                                                    })))))
                    .then(CommandManager.literal("setflag")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("flag", StringArgumentType.string())
                                            .then(CommandManager.argument("value", StringArgumentType.string())
                                                    .executes(context -> {
                                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                        String flag = StringArgumentType.getString(context, "flag").toLowerCase();
                                                        String value = StringArgumentType.getString(context, "value").toLowerCase();
                                                        PlayerStoryProgress progress = getProgress(player.getUuid());
                                                        if (value.equals("null")) {
                                                            progress.removeFlag(flag);
                                                        } else {
                                                            progress.setFlag(flag, value);
                                                        }
                                                        saveProgress(player.getUuid());
                                                        return 1;
                                                    })))))
                    .then(CommandManager.literal("getflag")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("flag", StringArgumentType.string())
                                            .executes(context -> {
                                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                String flag = StringArgumentType.getString(context, "flag").toLowerCase();
                                                PlayerStoryProgress progress = getProgress(player.getUuid());
                                                player.sendMessage(Text.literal("Flag '" + flag + "' of " + player.getName().getString() + " is '" + progress.getFlags().get(flag) + "'"));
                                                return 1;
                                            }))))
            );
        });
    }

    public PlayerStoryProgress getProgress(UUID playerUuid) {
        return playerProgress.computeIfAbsent(playerUuid, this::loadProgress);
    }

    private PlayerStoryProgress loadProgress(UUID playerUuid) {
        Path saveDir = server.getSavePath(WorldSavePath.ROOT).resolve("valdora/playerdata");
        Path file = saveDir.resolve(playerUuid.toString() + ".json");
        if (file.toFile().exists()) {
            try (Reader reader = new FileReader(file.toFile())) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> flags = gson.fromJson(reader, type);
                return new PlayerStoryProgress(flags);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new PlayerStoryProgress();
    }

    public void saveProgress(UUID playerUuid) {
        PlayerStoryProgress progress = playerProgress.get(playerUuid);
        if (progress != null) {
            Path saveDir = server.getSavePath(WorldSavePath.ROOT).resolve("valdora/playerdata");
            saveDir.toFile().mkdirs();
            Path file = saveDir.resolve(playerUuid.toString() + ".json");
            try (Writer writer = new FileWriter(file.toFile())) {
                gson.toJson(progress.getFlags(), writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PlayerStoryProgress {
        private final Map<String, String> flags = new HashMap<>();

        public PlayerStoryProgress() {}

        public PlayerStoryProgress(Map<String, String> flags) {
            this.flags.putAll(flags);
        }

        public boolean checkFlag(String flag, String value) {
            String currentValue = flags.get(flag);
            return currentValue != null && currentValue.equals(value);
        }

        public void setFlag(String flag, String value) {
            flags.put(flag, value);
        }

        public void removeFlag(String flag) {
            flags.remove(flag);
        }

        public Map<String, String> getFlags() {
            return flags;
        }
    }
}