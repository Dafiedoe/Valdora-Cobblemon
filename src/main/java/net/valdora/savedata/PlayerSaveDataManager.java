package net.valdora.savedata;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.valdora.Valdora;
import net.valdora.savedata.flaggedbarrier.PlayerFlagsS2CPayload;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerSaveDataManager {
    public static PlayerSaveDataManager INSTANCE;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final String PLAYER_SAVE_DATA_PATH = "valdora/playerdata/";

    private final Map<UUID, Map<String, PlayerStoryProgress>> loadedProfiles = new HashMap<>();
    private final Map<UUID, String> currentProfiles = new HashMap<>();

    public static final Identifier OPEN_PROFILE_GUI = Identifier.of("valdora", "open_profile_gui");
    public static final Identifier CREATE_PROFILE = Identifier.of("valdora", "create_profile");
    public static final Identifier SELECT_PROFILE = Identifier.of("valdora", "select_profile");
    public static final Identifier DELETE_PROFILE = Identifier.of("valdora", "delete_profile");
    public static final Identifier PROFILE_CREATION_RESULT = Identifier.of("valdora", "profile_creation_result");

    public PlayerSaveDataManager() {
        if (INSTANCE == null) {
            INSTANCE = this;
        }
    }

    public void register() {
        PayloadTypeRegistry.playC2S().register(CreateProfilePayload.ID, CreateProfilePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectProfilePayload.ID, SelectProfilePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteProfilePayload.ID, DeleteProfilePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenProfileGuiPayload.ID, OpenProfileGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProfileCreationResultPayload.ID, ProfileCreationResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerFlagsS2CPayload.PAYLOAD_ID, PlayerFlagsS2CPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            List<String> profiles = getProfiles(player.getServer(), player.getUuid());
            ServerPlayNetworking.send(player, new OpenProfileGuiPayload(profiles));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            String current = currentProfiles.get(uuid);
            if (current != null) {
                Map<String, PlayerStoryProgress> playerProfs = loadedProfiles.computeIfAbsent(uuid, k -> new HashMap<>());
                PlayerStoryProgress progress = playerProfs.computeIfAbsent(current, k -> loadProfile(player.getServer(), uuid, current));
                progress.setCoords(player.getX(), player.getY(), player.getZ());
                progress.setYawPitch(player.getYaw(), player.getPitch());

                List<PlayerStoryProgress.SimpleItem> mainOut = new ArrayList<>();
                for (int i = 0; i < player.getInventory().main.size(); i++) {
                    ItemStack s = player.getInventory().main.get(i);
                    if (s != null && !s.isEmpty()) {
                        Identifier id = Registries.ITEM.getId(s.getItem());
                        mainOut.add(new PlayerStoryProgress.SimpleItem(id.toString(), s.getCount()));
                    }
                }

                List<PlayerStoryProgress.SimpleItem> armorOut = new ArrayList<>();
                for (int i = 0; i < player.getInventory().armor.size(); i++) {
                    ItemStack s = player.getInventory().armor.get(i);
                    if (s != null && !s.isEmpty()) {
                        Identifier id = Registries.ITEM.getId(s.getItem());
                        armorOut.add(new PlayerStoryProgress.SimpleItem(id.toString(), s.getCount()));
                    }
                }

                List<PlayerStoryProgress.SimpleItem> offOut = new ArrayList<>();
                for (int i = 0; i < player.getInventory().offHand.size(); i++) {
                    ItemStack s = player.getInventory().offHand.get(i);
                    if (s != null && !s.isEmpty()) {
                        Identifier id = Registries.ITEM.getId(s.getItem());
                        offOut.add(new PlayerStoryProgress.SimpleItem(id.toString(), s.getCount()));
                    }
                }

                progress.setMainItems(mainOut);
                progress.setArmorItems(armorOut);
                progress.setOffhandItems(offOut);

                PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
                JsonArray partyJson = new JsonArray();
                for (int slot = 0; slot < party.size(); slot++) {
                    Pokemon pokemon = party.get(slot);
                    if (pokemon != null) {
                        partyJson.add(pokemon.saveToJSON(player.getRegistryManager(), new JsonObject()));
                    } else {
                        partyJson.add(JsonNull.INSTANCE);
                    }
                }

                progress.setParty(partyJson.toString());

                PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
                JsonArray pcJson = new JsonArray();
                int numBoxes = pc.getBoxes().size();
                for (int boxIndex = 0; boxIndex < numBoxes; boxIndex++) {
                    JsonArray boxJson = new JsonArray();
                    int occupiedCount = pc.getBoxes().get(boxIndex).getNonEmptySlots() != null ? pc.getBoxes().get(boxIndex).getNonEmptySlots().size() : 0;
                    int emptyCount = pc.getBoxes().get(boxIndex).getUnoccupiedSlots();
                    int slotCount = occupiedCount + emptyCount;
                    for (int slot = 0; slot < slotCount; slot++) {
                        Pokemon pokemon = pc.getBoxes().get(boxIndex).get(slot);
                        if (pokemon != null) {
                            boxJson.add(pokemon.saveToJSON(player.getRegistryManager(), new JsonObject()));
                        } else {
                            boxJson.add(JsonNull.INSTANCE);
                        }
                    }
                    pcJson.add(boxJson);
                }

                progress.setPc(pcJson.toString());

                saveProfile(player.getServer(), uuid, current, progress);
            }

            saveProgress(player.getServer(), uuid);
            loadedProfiles.remove(uuid);
            currentProfiles.remove(uuid);
        });

        ServerPlayNetworking.registerGlobalReceiver(CreateProfilePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                createProfile(player.getServer(), player.getUuid(), payload.profileName(), player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SelectProfilePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            try {
                selectProfile(player.getServer(), player.getUuid(), payload.profileName());
                context.server().execute(() -> {
                    player.sendMessage(Text.literal("Selected profile '" + payload.profileName() + "'."), false);
                    PlayerStoryProgress progress = getProgress(player.getServer(), player.getUuid());
                    try {
                        ServerWorld serverWorld = (ServerWorld) player.getWorld();
                        player.teleport(serverWorld, progress.getX(), progress.getY(), progress.getZ(), progress.getYaw(), progress.getPitch());
                    } catch (Throwable t) {
                        Valdora.LOGGER.error("Error teleporting player: {}", t.getMessage(), t);
                    }

                    player.getInventory().clear();

                    List<PlayerStoryProgress.SimpleItem> mainList = progress.getMainItems();
                    for (int i = 0; i < mainList.size() && i < player.getInventory().main.size(); i++) {
                        PlayerStoryProgress.SimpleItem si = mainList.get(i);
                        try {
                            Item item = Registries.ITEM.get(Identifier.of(si.item));
                            if (item != null) {
                                ItemStack stack = new ItemStack(item, Math.max(0, si.count));
                                player.getInventory().setStack(i, stack);
                            }
                        } catch (Exception ex) {
                            Valdora.LOGGER.error("Error restoring main inventory item: {}", ex.getMessage(), ex);
                        }
                    }

                    List<PlayerStoryProgress.SimpleItem> armorList = progress.getArmorItems();
                    for (int i = 0; i < armorList.size() && i < player.getInventory().armor.size(); i++) {
                        PlayerStoryProgress.SimpleItem si = armorList.get(i);
                        try {
                            Item item = Registries.ITEM.get(Identifier.of(si.item));
                            if (item != null) {
                                ItemStack stack = new ItemStack(item, Math.max(0, si.count));
                                player.getInventory().armor.set(i, stack);
                            }
                        } catch (Exception ex) {
                            Valdora.LOGGER.error("Error restoring armor item: {}", ex.getMessage(), ex);
                        }
                    }

                    List<PlayerStoryProgress.SimpleItem> off = progress.getOffhandItems();
                    if (!off.isEmpty() && player.getInventory().offHand.size() > 0) {
                        PlayerStoryProgress.SimpleItem si = off.get(0);
                        try {
                            Item item = Registries.ITEM.get(Identifier.of(si.item));
                            if (item != null) {
                                ItemStack stack = new ItemStack(item, Math.max(0, si.count));
                                player.getInventory().offHand.set(0, stack);
                            }
                        } catch (Exception ex) {
                            Valdora.LOGGER.error("Error restoring offhand item: {}", ex.getMessage(), ex);
                        }
                    }

                    PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
                    party.clearParty();

                    if (progress.getParty() != null && !progress.getParty().isEmpty()) {
                        JsonArray partyJson = JsonParser.parseString(progress.getParty()).getAsJsonArray();

                        for (int slot = 0; slot < Math.min(6, partyJson.size()); slot++) {
                            JsonElement element = partyJson.get(slot);
                            if (!element.isJsonNull() && element.isJsonObject()) {
                                Pokemon pokemon = new Pokemon();
                                pokemon.loadFromJSON(player.getRegistryManager(), element.getAsJsonObject());
                                party.set(slot, pokemon);
                            }
                        }
                    }

                    PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
                    pc.clearPC();

                    if (progress.getPc() != null && !progress.getPc().isEmpty()) {
                        JsonArray pcJson = JsonParser.parseString(progress.getPc()).getAsJsonArray();

                        for (int boxNum = 0; boxNum < pc.getBoxes().size(); boxNum++) {
                            JsonArray boxJson = pcJson.get(boxNum).getAsJsonArray();
                            int occupiedCount = pc.getBoxes().get(boxNum).getNonEmptySlots() != null ? pc.getBoxes().get(boxNum).getNonEmptySlots().size() : 0;
                            int emptyCount = pc.getBoxes().get(boxNum).getUnoccupiedSlots();
                            int slotCount = occupiedCount + emptyCount;
                            for (int slot = 0; slot < slotCount; slot++) {
                                JsonElement element = boxJson.get(slot);
                                if (!element.isJsonNull() && element.isJsonObject()) {
                                    Pokemon pokemon = new Pokemon();
                                    pokemon.loadFromJSON(player.getRegistryManager(), element.getAsJsonObject());
                                    pc.set(new PCPosition(boxNum, slot), pokemon);
                                    Valdora.LOGGER.info("Setting boxNum: {}, slot: {}, pokemon: {}", boxNum, slot, pokemon.getSpecies().getName());
                                }
                            }
                        }
                    }

                    player.closeHandledScreen();
                    sendFlagsToClient(player);
                });
            } catch (IllegalArgumentException e) {
                context.server().execute(() -> player.sendMessage(Text.literal(e.getMessage()), false));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(DeleteProfilePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            try {
                deleteProfile(player.getServer(), player.getUuid(), payload.profileName());
                context.server().execute(() -> {
                    List<String> profiles = getProfiles(player.getServer(), player.getUuid());
                    ServerPlayNetworking.send(player, new OpenProfileGuiPayload(profiles));
                    player.sendMessage(Text.literal("Profile '" + payload.profileName() + "' deleted."), false);
                });
            } catch (IllegalArgumentException e) {
                context.server().execute(() -> player.sendMessage(Text.literal(e.getMessage()), false));
            }
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
                                                        PlayerStoryProgress progress = getProgress(player.getServer(), player.getUuid());
                                                        String command;
                                                        if (progress.getFlags().containsKey(flag)) {
                                                            command = "easy_npc dialog open " + fromUUID + " " + player.getName().getString() + " " + flag + "_" + progress.getFlags().get(flag);
                                                        } else {
                                                            command = "easy_npc dialog open " + fromUUID + " " + player.getName().getString() + " null_" + flag;
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
                                                                PlayerStoryProgress progress = getProgress(player.getServer(), player.getUuid());
                                                                if (value.equals("null")) {
                                                                    progress.removeFlag(flag);
                                                                } else {
                                                                    progress.setFlag(flag, value);
                                                                }
                                                                saveProgress(player.getServer(), player.getUuid());
                                                                sendFlagsToClient(player);
                                                                return 1;
                                                            })))))
                            .then(CommandManager.literal("getflag")
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .then(CommandManager.argument("flag", StringArgumentType.string())
                                                    .executes(context -> {
                                                        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                                        String flag = StringArgumentType.getString(context, "flag").toLowerCase();
                                                        ServerPlayerEntity executor = context.getSource().getPlayer();
                                                        PlayerStoryProgress progress = getProgress(targetPlayer.getServer(), targetPlayer.getUuid());
                                                        executor.sendMessage(Text.literal("Flag '" + flag + "' of " + targetPlayer.getName().getString() + " is '" + progress.getFlags().get(flag) + "'"));
                                                        return 1;
                                                    }))))
                                    .then(CommandManager.literal("clearallflags")
                                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                                    .executes(context -> {
                                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                        PlayerStoryProgress progress = getProgress(player.getServer(), player.getUuid());
                                                        progress.getFlags().clear();
                                                        saveProgress(player.getServer(), player.getUuid());
                                                        sendFlagsToClient(player);
                                                        player.sendMessage(Text.literal("All flags cleared for " + player.getName().getString()));
                                                        return 1;
                                                    }))));
        });
    }

    public PlayerStoryProgress getProgress(MinecraftServer server, UUID playerUuid) {
        String current = currentProfiles.get(playerUuid);
        if (current == null) {
            return new PlayerStoryProgress();
        }
        Map<String, PlayerStoryProgress> playerProfs = loadedProfiles.computeIfAbsent(playerUuid, k -> new HashMap<>());
        return playerProfs.computeIfAbsent(current, k -> loadProfile(server, playerUuid, current));
    }

    private PlayerStoryProgress loadProfile(MinecraftServer server, UUID playerUuid, String profileName) {
        Path playerDir = getPlayerDir(server, playerUuid);
        Path file = playerDir.resolve(profileName + ".json");
        if (file.toFile().exists()) {
            try (Reader reader = new FileReader(file.toFile())) {
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> json = gson.fromJson(reader, type);

                Map<String, String> flags = new HashMap<>();
                Object flagsObj = json.get("flags");
                if (flagsObj instanceof Map) {
                    Map<?, ?> rawFlags = (Map<?, ?>) flagsObj;
                    for (Map.Entry<?, ?> e : rawFlags.entrySet()) {
                        if (e.getKey() != null && e.getValue() != null) {
                            flags.put(e.getKey().toString(), e.getValue().toString());
                        }
                    }
                }

                double x = 0, y = 0, z = 0;
                Object xo = json.get("x");
                Object yo = json.get("y");
                Object zo = json.get("z");
                if (xo instanceof Number) x = ((Number) xo).doubleValue();
                if (yo instanceof Number) y = ((Number) yo).doubleValue();
                if (zo instanceof Number) z = ((Number) zo).doubleValue();

                float yaw = 0f, pitch = 0f;
                Object yawObj = json.get("yaw");
                Object pitchObj = json.get("pitch");
                if (yawObj instanceof Number) yaw = ((Number) yawObj).floatValue();
                if (pitchObj instanceof Number) pitch = ((Number) pitchObj).floatValue();

                List<PlayerStoryProgress.SimpleItem> main = new ArrayList<>();
                List<PlayerStoryProgress.SimpleItem> armor = new ArrayList<>();
                List<PlayerStoryProgress.SimpleItem> offhand = new ArrayList<>();

                Object invObj = json.get("inventory");
                if (invObj instanceof Map) {
                    Map<?, ?> invMap = (Map<?, ?>) invObj;

                    Object mainObj = invMap.get("main");
                    if (mainObj instanceof List) {
                        for (Object entry : (List<?>) mainObj) {
                            if (!(entry instanceof Map)) continue;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) entry;
                            String id = m.getOrDefault("item", "").toString();
                            int count = 0;
                            Object cnt = m.get("count");
                            if (cnt instanceof Number) count = ((Number) cnt).intValue();
                            else if (cnt != null) {
                                try { count = Integer.parseInt(cnt.toString()); } catch (Exception ignored) {}
                            }
                            if (!id.isEmpty()) main.add(new PlayerStoryProgress.SimpleItem(id, count));
                        }
                    }

                    Object armorObj = invMap.get("armor");
                    if (armorObj instanceof List) {
                        for (Object entry : (List<?>) armorObj) {
                            if (!(entry instanceof Map)) continue;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) entry;
                            String id = m.getOrDefault("item", "").toString();
                            int count = 0;
                            Object cnt = m.get("count");
                            if (cnt instanceof Number) count = ((Number) cnt).intValue();
                            else if (cnt != null) {
                                try { count = Integer.parseInt(cnt.toString()); } catch (Exception ignored) {}
                            }
                            if (!id.isEmpty()) armor.add(new PlayerStoryProgress.SimpleItem(id, count));
                        }
                    }

                    Object offObj = invMap.get("offhand");
                    if (offObj instanceof List) {
                        for (Object entry : (List<?>) offObj) {
                            if (!(entry instanceof Map)) continue;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) entry;
                            String id = m.getOrDefault("item", "").toString();
                            int count = 0;
                            Object cnt = m.get("count");
                            if (cnt instanceof Number) count = ((Number) cnt).intValue();
                            else if (cnt != null) {
                                try { count = Integer.parseInt(cnt.toString()); } catch (Exception ignored) {}
                            }
                            if (!id.isEmpty()) offhand.add(new PlayerStoryProgress.SimpleItem(id, count));
                        }
                    }
                }

                String party = "";
                Object partyObj = json.get("party");
                if (partyObj instanceof String) {
                    party = (String) partyObj;
                }

                String pc = "";
                Object pcObj = json.get("pc");
                if (pcObj instanceof String) {
                    pc = (String) pcObj;
                }

                String cp = "";
                Object cpObj = json.get("lastcheckpoint");
                if (cpObj instanceof String) {
                    cp = (String) cpObj;
                }

                return new PlayerStoryProgress(flags, x, y, z, yaw, pitch, main, armor, offhand, party, pc, cp);
            } catch (IOException e) {
                Valdora.LOGGER.error("Error loading profile {} for player {}: {}", profileName, playerUuid, e.getMessage(), e);
            }
        }
        return new PlayerStoryProgress();
    }

    private void saveProfile(MinecraftServer server, UUID playerUuid, String profileName, PlayerStoryProgress progress) {
        Path playerDir = getPlayerDir(server, playerUuid);
        playerDir.toFile().mkdirs();
        Path file = playerDir.resolve(profileName + ".json");

        try (Writer writer = new FileWriter(file.toFile())) {
            Map<String, Object> json = new HashMap<>();
            json.put("flags", progress.getFlags());
            json.put("x", progress.getX());
            json.put("y", progress.getY());
            json.put("z", progress.getZ());
            json.put("yaw", progress.getYaw());
            json.put("pitch", progress.getPitch());

            json.put("inventory", Map.of(
                    "main", progress.getMainItems(),
                    "armor", progress.getArmorItems(),
                    "offhand", progress.getOffhandItems()
            ));

            json.put("party", progress.getParty());
            json.put("pc", progress.getPc());

            json.put("lastcheckpoint", progress.getLastCheckPoint());

            gson.toJson(json, writer);
        } catch (IOException e) {
            Valdora.LOGGER.error("Error saving profile {} for player {}: {}", profileName, playerUuid, e.getMessage(), e);
        }
    }

    public void saveProgress(MinecraftServer server, UUID playerUuid) {
        String current = currentProfiles.get(playerUuid);
        if (current != null) {
            Map<String, PlayerStoryProgress> playerProfs = loadedProfiles.get(playerUuid);
            if (playerProfs != null) {
                PlayerStoryProgress progress = playerProfs.get(current);
                if (progress != null) {
                    saveProfile(server, playerUuid, current, progress);
                }
            }
        }
        sendFlagsToClient(server.getPlayerManager().getPlayer(playerUuid));
    }

    private void sendFlagsToClient(ServerPlayerEntity player) {
        try {
            Map<String, String> flags;
            String current = currentProfiles.get(player.getUuid());
            if (current == null) {
                flags = new HashMap<>();
            } else {
                PlayerStoryProgress progress = getProgress(player.getServer(), player.getUuid());
                flags = new HashMap<>(progress.getFlags());
            }
            PlayerFlagsS2CPayload payload = new PlayerFlagsS2CPayload(player.getUuid(), flags);
            ServerPlayNetworking.send(player, payload);
        } catch (Throwable t) {
            Valdora.LOGGER.error("Error sending flags to client for player {}: {}", player.getUuid(), t.getMessage(), t);
        }
    }

    private Path getPlayerDir(MinecraftServer server, UUID playerUuid) {
        return Paths.get(PLAYER_SAVE_DATA_PATH + playerUuid.toString());
    }

    public void createProfile(MinecraftServer server, UUID playerUuid, String profileName, ServerPlayerEntity player) {
        if (profileName == null || profileName.trim().isEmpty()) {
            ServerPlayNetworking.send(player, new ProfileCreationResultPayload(false, "Profile name cannot be empty"));
            return;
        }
        Path playerDir = getPlayerDir(server, playerUuid);
        playerDir.toFile().mkdirs();
        Path file = playerDir.resolve(profileName + ".json");
        if (file.toFile().exists()) {
            ServerPlayNetworking.send(player, new ProfileCreationResultPayload(false, "Profile '" + profileName + "' already exists"));
            return;
        }
        if (getProfiles(server, playerUuid).size() >= 5) { // MAX_PROFILES = 5
            ServerPlayNetworking.send(player, new ProfileCreationResultPayload(false, "Maximum profiles reached (5)"));
            return;
        }
        PlayerStoryProgress progress = new PlayerStoryProgress();
        saveProfile(server, playerUuid, profileName, progress);
        ServerPlayNetworking.send(player, new ProfileCreationResultPayload(true, ""));
        ServerPlayNetworking.send(player, new OpenProfileGuiPayload(getProfiles(server, playerUuid)));
        Valdora.LOGGER.info("Created profile '{}' for player {}", profileName, playerUuid);
    }

    public void selectProfile(MinecraftServer server, UUID playerUuid, String profileName) {
        Path file = getPlayerDir(server, playerUuid).resolve(profileName + ".json");
        if (!file.toFile().exists()) {
            throw new IllegalArgumentException("Profile does not exist");
        }
        currentProfiles.put(playerUuid, profileName);
        getProgress(server, playerUuid);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (player != null) {
            sendFlagsToClient(player);
        }
    }

    public void deleteProfile(MinecraftServer server, UUID playerUuid, String profileName) {
        Path file = getPlayerDir(server, playerUuid).resolve(profileName + ".json");
        if (file.toFile().exists()) file.toFile().delete();
        Map<String, PlayerStoryProgress> playerProfs = loadedProfiles.get(playerUuid);
        if (playerProfs != null) playerProfs.remove(profileName);
        String current = currentProfiles.get(playerUuid);
        if (current != null && current.equals(profileName)) {
            currentProfiles.remove(playerUuid);
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) sendFlagsToClient(player);
        }
    }

    public List<String> getProfiles(MinecraftServer server, UUID playerUuid) {
        Path playerDir = getPlayerDir(server, playerUuid);
        File dir = playerDir.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dir.listFiles())
                .filter(f -> f.isFile() && f.getName().endsWith(".json"))
                .map(f -> f.getName().substring(0, f.getName().length() - 5))
                .collect(Collectors.toList());
    }

    public record OpenProfileGuiPayload(List<String> profiles) implements CustomPayload {
        public static final Id<OpenProfileGuiPayload> ID = new Id<>(OPEN_PROFILE_GUI);
        public static final PacketCodec<RegistryByteBuf, OpenProfileGuiPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING.collect(PacketCodecs.toList()), OpenProfileGuiPayload::profiles,
                OpenProfileGuiPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record CreateProfilePayload(String profileName) implements CustomPayload {
        public static final Id<CreateProfilePayload> ID = new Id<>(CREATE_PROFILE);
        public static final PacketCodec<RegistryByteBuf, CreateProfilePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, CreateProfilePayload::profileName,
                CreateProfilePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SelectProfilePayload(String profileName) implements CustomPayload {
        public static final Id<SelectProfilePayload> ID = new Id<>(SELECT_PROFILE);
        public static final PacketCodec<RegistryByteBuf, SelectProfilePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, SelectProfilePayload::profileName,
                SelectProfilePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record DeleteProfilePayload(String profileName) implements CustomPayload {
        public static final Id<DeleteProfilePayload> ID = new Id<>(DELETE_PROFILE);
        public static final PacketCodec<RegistryByteBuf, DeleteProfilePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, DeleteProfilePayload::profileName,
                DeleteProfilePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ProfileCreationResultPayload(boolean success, String errorMessage) implements CustomPayload {
        public static final Id<ProfileCreationResultPayload> ID = new Id<>(PROFILE_CREATION_RESULT);
        public static final PacketCodec<RegistryByteBuf, ProfileCreationResultPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, ProfileCreationResultPayload::success,
                PacketCodecs.STRING, ProfileCreationResultPayload::errorMessage,
                ProfileCreationResultPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static class PlayerStoryProgress {
        private final Map<String, String> flags = new HashMap<>();
        private double x = Valdora.WORLD_SPAWN_X;
        private double y = Valdora.WORLD_SPAWN_Y;
        private double z = Valdora.WORLD_SPAWN_Z;
        private float yaw = Valdora.WORLD_SPAWN_YAW;
        private float pitch = Valdora.WORLD_SPAWN_PITCH;
        private final List<SimpleItem> main = new ArrayList<>();
        private final List<SimpleItem> armor = new ArrayList<>();
        private final List<SimpleItem> offhand = new ArrayList<>();
        private String party;
        private String pc;
        private String lastCheckPoint;

        public PlayerStoryProgress() {}

        public PlayerStoryProgress(Map<String, String> flags, double x, double y, double z, float yaw, float pitch,
                                   List<SimpleItem> main, List<SimpleItem> armor, List<SimpleItem> offhand, String party, String pc, String cp) {
            this.flags.putAll(flags);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            if (main != null) this.main.addAll(main);
            if (armor != null) this.armor.addAll(armor);
            if (offhand != null) this.offhand.addAll(offhand);
            this.party = party;
            this.pc = pc;
            this.lastCheckPoint = cp;
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

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public void setCoords(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
        public void setYawPitch(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }

        public String getParty() { return party; }
        public String getPc() { return pc; }

        public String getLastCheckPoint() { return lastCheckPoint; }

        public List<SimpleItem> getMainItems() {
            return new ArrayList<>(main);
        }

        public List<SimpleItem> getArmorItems() {
            return new ArrayList<>(armor);
        }

        public List<SimpleItem> getOffhandItems() {
            return new ArrayList<>(offhand);
        }

        public void setMainItems(List<SimpleItem> items) {
            main.clear();
            if (items != null) main.addAll(items);
        }

        public void setArmorItems(List<SimpleItem> items) {
            armor.clear();
            if (items != null) armor.addAll(items);
        }

        public void setOffhandItems(List<SimpleItem> items) {
            offhand.clear();
            if (items != null) offhand.addAll(items);
        }

        public void setParty(String party) {
            this.party = party;
        }

        public void setPc(String pc) {
            this.pc = pc;
        }

        public void setLastCheckPoint(String cp) {
            this.lastCheckPoint = cp;
        }

        public static class SimpleItem {
            public String item;
            public int count;

            public SimpleItem(String item, int count) {
                this.item = item;
                this.count = count;
            }
        }
    }
}