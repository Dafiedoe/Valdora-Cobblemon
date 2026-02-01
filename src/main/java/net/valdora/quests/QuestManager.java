package net.valdora.quests;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import com.google.gson.*;
import com.mojang.brigadier.ParseResults;
import kotlin.Unit;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.valdora.Valdora;
import net.valdora.quests.commands.*;
import net.valdora.quests.hud.CompassTargetPayload;
import net.valdora.quests.hud.QuestHudPayload;
import net.valdora.quests.objectivetypes.*;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.utils.TickScheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class QuestManager {
    private static final String QUEST_CONFIG_PATH = "config/valdora/quests";
    private static final Map<String, Quest> QUESTS = new HashMap<>();
    
    public static void register() {
        TickScheduler.runNextTick(1, QuestManager::load);
        
        PayloadTypeRegistry.playS2C().register(QuestHudPayload.ID, QuestHudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CompassTargetPayload.ID, CompassTargetPayload.CODEC);
        
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            QuestStartCommand.register(dispatcher);
            ProgressQuestCommand.register(dispatcher);
            ForceProgressQuestCommand.register(dispatcher);
            DeliverItemQuestCommand.register(dispatcher);
            DeliverPokemonQuestCommand.register(dispatcher);
            TrackQuestCommand.register(dispatcher);
        }));
        
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Vec3d pos = player.getPos();
                updateQuestProgress(player, ObjectiveType.REACH_LOCATION, pos);
            }
        });
        
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, QuestManager::handlePokemonCapturedEvent);
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.NORMAL, QuestManager::handlePokemonEvolutionEvent);
    }
    
    private static Unit handlePokemonCapturedEvent(PokemonCapturedEvent event) {
        updateQuestProgress(event.getPlayer(), ObjectiveType.CATCH_POKEMON, event.getPokemon());
        
        return Unit.INSTANCE;
    }
    
    private static Unit handlePokemonEvolutionEvent(EvolutionCompleteEvent event) {
        updateQuestProgress(event.getPokemon().getOwnerPlayer(), ObjectiveType.EVOLVE_POKEMON, event.getPokemon().getPreEvolution().getSpecies());
        
        return Unit.INSTANCE;
    }
    
    public static void load() {
        QUESTS.clear();
        
        try {
            Path configPath = Paths.get(QUEST_CONFIG_PATH);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                Valdora.LOGGER.info("Created quest config directory: " + configPath);
                return;
            }
            
            Files.walk(configPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String jsonContent = Files.readString(path);
                            JsonParser parser = new JsonParser();
                            JsonObject json = parser.parse(jsonContent).getAsJsonObject();
                            
                            String id = json.get("id").getAsString();
                            String title = json.get("title").getAsString();
                            String description = json.get("description").getAsString();
                            
                            List<Objective> objectives = new ArrayList<>();
                            if (json.has("objectives")) {
                                JsonArray objectivesArray = json.getAsJsonArray("objectives").getAsJsonArray();
                                for (JsonElement element : objectivesArray) {
                                    JsonObject objJson = element.getAsJsonObject();
                                    
                                    String objTitle = objJson.get("title").getAsString();
                                    String objDescription = objJson.get("description").getAsString();
                                    String typeStr = objJson.get("type").getAsString().toUpperCase();
                                    ObjectiveType type = ObjectiveType.valueOf(typeStr);
                                    
                                    Objective objective = null;
                                    
                                    switch (type) {
                                        case REACH_LOCATION ->
                                                objective = new ReachLocationObjective(objTitle, objDescription, id, objJson);
                                        case COMMAND_DRIVEN ->
                                                objective = new CommandDrivenObjective(objTitle, objDescription, id, objJson);
                                        case DELIVER_ITEM ->
                                                objective = new DeliverItemObjective(objTitle, objDescription, id, objJson);
                                        case DELIVER_POKEMON ->
                                                objective = new DeliverPokemonObjective(objTitle, objDescription, id, objJson);
                                        case DEFEAT_POKEMON ->
                                                objective = new DefeatPokemonObjective(objTitle, objDescription, id, objJson);
                                        case CATCH_POKEMON ->
                                                objective = new CatchPokemonObjective(objTitle, objDescription, id, objJson);
                                        case EVOLVE_POKEMON ->
                                                objective = new EvolvePokemonObjective(objTitle, objDescription, id, objJson);
                                        case DEFEAT_TRAINER ->
                                                objective = new DefeatTrainersObjective(objTitle, objDescription, id, objJson);
                                    }
                                    
                                    if (objective == null) {
                                        continue;
                                    }
                                    
                                    if (objJson.has("completionCommands")) {
                                        JsonArray commandsArray = objJson.getAsJsonArray("completionCommands");
                                        List<String> completionCommands = new ArrayList<>();
                                        
                                        for (JsonElement cmdElement : commandsArray) {
                                            completionCommands.add(cmdElement.getAsString());
                                        }
                                        
                                        objective.setCompletionCommands(completionCommands);
                                    } else {
                                        objective.setCompletionCommands(new ArrayList<>());
                                    }
                                    
                                    if (objJson.has("count")) {
                                        objective.count = objJson.get("count").getAsInt();
                                    }
                                    
                                    objectives.add(objective);
                                }
                            }
                            
                            Quest quest = new Quest();
                            quest.id = id;
                            quest.title = title;
                            quest.description = description;
                            quest.objectives = objectives;
                            
                            QUESTS.put(id, quest);
                            Valdora.LOGGER.info("Loaded quest: " + id);
                        } catch (IOException e) {
                            Valdora.LOGGER.error("Failed to read quest file " + path + ": " + e.getMessage());
                        } catch (JsonParseException e) {
                            Valdora.LOGGER.error("Failed to parse quest JSON in " + path + ": " + e.getMessage());
                        } catch (IllegalArgumentException e) {
                            Valdora.LOGGER.error("Invalid objective type in quest file " + path + ": " + e.getMessage());
                        }
                    });
            
            Valdora.LOGGER.info("Successfully loaded " + QUESTS.size() + " quests");
        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to access quest config directory: " + e.getMessage());
        }
    }
    
    public static Quest getQuestById(String id) {
        if (QUESTS.containsKey(id)) {
            return QUESTS.get(id);
        }
        return null;
    }
    
    public static Map<String, Quest> getAllQuests() {
        return QUESTS;
    }
    
    public static void updateQuestProgress(ServerPlayerEntity player, ObjectiveType type, Object data) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        
        if (progress == null) {
            Valdora.LOGGER.error(player.getName().getString() + " has no valid save data!");
            return;
        }
        
        List<ActiveQuest> activeQuests = progress.getActiveQuests();
        
        if (activeQuests == null) {
            return;
        }
        
        if (type.equals(ObjectiveType.FORCED) && data instanceof String dataStr) {
            Quest quest = getQuestById(dataStr);
            
            if (quest == null) return;
            
            for (ActiveQuest activeQuest : new ArrayList<>(activeQuests)) {
                if (activeQuest.questId.equals(dataStr)) {
                    handleObjectiveCompletion(player, progress, activeQuest, quest);
                }
            }
        }
        
        for (ActiveQuest activeQuest : new ArrayList<>(activeQuests)) {
            Quest quest = getQuestById(activeQuest.questId);
            
            if (quest != null && !quest.id.equals(activeQuest.questId)) {
                continue;
            }
            
            Objective currentObjective = quest.getObjectiveByIndex(activeQuest.objectiveIndex);
            if (currentObjective == null) {
                continue;
            }
            
            if (!currentObjective.type.equals(type)) {
                continue;
            }
            
            boolean objectiveCompleted = currentObjective.handleObjectiveUpdate(activeQuest, player, data);
            
            if (objectiveCompleted) {
                handleObjectiveCompletion(player, progress, activeQuest, quest);
            } else if (progress.getTrackingQuest().equals(quest.id)) {
                sendQuestHudUpdate(player, quest, activeQuest.objectiveIndex, activeQuest.count, quest.getObjectiveByIndex(activeQuest.objectiveIndex).count);
            }
        }
    }
    
    private static void handleObjectiveCompletion(ServerPlayerEntity player, PlayerSaveDataManager.PlayerStoryProgress progress, ActiveQuest activeQuest, Quest quest) {
        Objective currentObjective = quest.getObjectiveByIndex(activeQuest.objectiveIndex);
        ServerCommandSource source = player.getCommandSource().withLevel(4);
        
        for (String command : currentObjective.completionCommands) {
            String processedCommand = command.replace("@player", player.getName().getString());
            
            ParseResults<ServerCommandSource> parseResults =
                    source.getServer().getCommandManager().getDispatcher().parse(processedCommand, source);
            
            source.getServer().getCommandManager().execute(parseResults, processedCommand);
        }
        
        if (activeQuest.objectiveIndex + 1 >= quest.objectives.size()) {
            progress.addCompletedQuest(activeQuest.questId);
            player.sendMessage(Text.literal("Quest completed: " + quest.title));
            
            if (progress.getTrackingQuest().equals(quest.id)) {
                sendQuestCompleteHudUpdate(player);
            }
        } else {
            activeQuest.objectiveIndex++;
            activeQuest.count = 0;
            Objective nextObjective = quest.getObjectiveByIndex(activeQuest.objectiveIndex);
            if (nextObjective != null) {
                player.sendMessage(Text.literal("Objective completed! Next: " + nextObjective.title));
                if (progress.getTrackingQuest().equals(quest.id)) {
                    sendQuestHudUpdate(player, quest, activeQuest.objectiveIndex, activeQuest.count, quest.getObjectiveByIndex(activeQuest.objectiveIndex).count);
                    if (nextObjective instanceof ReachLocationObjective reachLocationObjective) {
                        sendCompassUpdate(player, new Vec3d(reachLocationObjective.x, reachLocationObjective.y, reachLocationObjective.z), reachLocationObjective.showCompass);
                    } else {
                        sendCompassUpdate(player, Vec3d.ZERO, false);
                    }
                }
            }
        }
        PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());
    }
    
    public static void startQuest(ServerPlayerEntity player, String id) {
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        
        Quest quest = getQuestById(id);
        
        if (progress == null) {
            Valdora.LOGGER.error(player.getName().getString() + " has no valid save data!");
            return;
        }
        
        if (quest == null) {
            Valdora.LOGGER.error("Quest with id '" + id + "' doesn't exist");
            return;
        }
        
        if (progress.hasCompletedQuest(id)) {
            Valdora.LOGGER.info(player.getName().getString() + " has already completed quest '" + id + "'");
            return;
        }
        
        if (progress.getActiveQuestById(id) != null) {
            Valdora.LOGGER.info(player.getName().getString() + " already has quest '" + id + "' active");
            return;
        }
        
        progress.addActiveQuest(new ActiveQuest(id, 0, 0));
        
        player.sendMessage(Text.literal("§lQuest started: " + quest.title));
        player.sendMessage(Text.literal(quest.description));
        player.sendMessage(Text.literal(""));
        player.sendMessage(Text.literal(quest.objectives.get(0).title));
        
        if (progress.getTrackingQuest().isEmpty()) {
            progress.setTrackingQuest(player, quest);
            
            sendQuestHudUpdate(player, quest, 0, 0, quest.getObjectiveByIndex(0).count);

            /*
            if (quest.getObjectives().get(0) instanceof ReachLocationObjective reachLocationObjective) {
                sendCompassUpdate(player, new Vec3d(reachLocationObjective.x, reachLocationObjective.y, reachLocationObjective.z), reachLocationObjective.showCompass);
            } else {
                sendCompassUpdate(player, Vec3d.ZERO, false);
            }
             */
        }
        
        PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());
    }
    
    public static void sendQuestCompleteHudUpdate(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new QuestHudPayload("", "", -1, -1));
        ServerPlayNetworking.send(player, new CompassTargetPayload(0, 0, 0, false));
    }
    
    public static void sendQuestHudUpdate(ServerPlayerEntity player, Quest quest, int objectiveIndex, int curCount, int reqCount) {
        ServerPlayNetworking.send(player, new QuestHudPayload(quest.title, quest.objectives.get(objectiveIndex).title, curCount, reqCount));
    }
    
    public static void sendCompassUpdate(ServerPlayerEntity player, Vec3d pos, boolean showMarker) {
        ServerPlayNetworking.send(player, new CompassTargetPayload(pos.getX(), pos.getY(), pos.getZ(), showMarker));
    }
}
