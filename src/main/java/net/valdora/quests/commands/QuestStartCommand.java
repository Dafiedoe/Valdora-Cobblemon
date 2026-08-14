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
import net.valdora.quests.QuestManager;

public class QuestStartCommand {
    private static final SuggestionProvider<ServerCommandSource> QUEST_SUGGESTIONS = (context, builder) -> {
        QuestManager.getAllQuests().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora")
                .then(CommandManager.literal("startquest")
                        .requires(source -> Permissions.check(source, "valdora.quests", 2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("questId", StringArgumentType.string())
                                        .suggests(QUEST_SUGGESTIONS)
                                        .executes(QuestStartCommand::execute)))));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String questId = StringArgumentType.getString(context, "questId");
        QuestManager.startQuest(player, questId);
        return 1;
    }
}