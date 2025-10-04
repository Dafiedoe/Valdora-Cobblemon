package net.valdora.quests.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.quests.ObjectiveType;
import net.valdora.quests.QuestManager;
import net.valdora.savedata.PlayerSaveDataManager;

public class DeliverPokemonQuestCommand {
    private static final SuggestionProvider<ServerCommandSource> ACTIVE_QUEST_SUGGESTIONS = (context, builder) -> {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        progress.getActiveQuests().forEach(aq -> builder.suggest(aq.questId));
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora")
                .then(CommandManager.literal("deliver_pokemon")
                        .requires(source -> Permissions.check(source, "valdora.quests", 2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("questId", StringArgumentType.string())
                                        .suggests(ACTIVE_QUEST_SUGGESTIONS)
                                        .executes(DeliverPokemonQuestCommand::execute)))));
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String questId = StringArgumentType.getString(context, "questId");
        QuestManager.updateQuestProgress(player, ObjectiveType.DELIVER_POKEMON, questId);
        return 1;
    }
}
