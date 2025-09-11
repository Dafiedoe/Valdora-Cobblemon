package net.valdora.savedata.flaggedbarrier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.valdora.general.ModBlocks;
import net.valdora.general.ModComponents;

public class GetBarrierCommand {
    private static final SuggestionProvider<ServerCommandSource> FLAG_SUGGESTIONS = (context, builder) -> {
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> VALUE_SUGGESTIONS = (context, builder) -> {
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("valdora")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                CommandManager.literal("getbarrier")
                                        .then(
                                                CommandManager.argument("flag", StringArgumentType.string())
                                                        .suggests(FLAG_SUGGESTIONS)
                                                        .then(
                                                                CommandManager.argument("value", StringArgumentType.string())
                                                                        .suggests(VALUE_SUGGESTIONS)
                                                                        .executes(GetBarrierCommand::execute)
                                                        )
                                        )
                        )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command can only be run by a player."));
            return 0;
        }

        String flag = StringArgumentType.getString(context, "flag");
        String value = StringArgumentType.getString(context, "value");

        ItemStack stack = new ItemStack(ModBlocks.FLAGGED_BARRIER);
        stack.set(ModComponents.FLAG_NAME, flag);
        stack.set(ModComponents.FLAG_VALUE, value);

        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        source.sendFeedback(
                () -> Text.literal("Given flagged barrier with flag '" + flag + "' and value '" + value + "'"),
                false
        );
        return Command.SINGLE_SUCCESS;
    }
}
