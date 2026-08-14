package net.valdora.timespecificevents;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.valdora.utils.PokemonUtils;

public class ShinyDebugCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("valdora")
                        .requires(source -> source.hasPermissionLevel(2) && source.getEntity() instanceof ServerPlayerEntity)
                        .then(
                                CommandManager.literal("debugshiny")
                                        .then(
                                                CommandManager.argument("rollamount", IntegerArgumentType.integer(1))
                                                        .executes(ShinyDebugCommand::execute)
                                        )
                        )
        );
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        int rollAmount = IntegerArgumentType.getInteger(context, "rollamount");
        int shinyAmount = 0;
        for (int i = 0; i < rollAmount; i++) {
            if (PokemonUtils.rollForShiny(player)) {
                shinyAmount++;
            }
        }
        player.sendMessage(Text.literal("Rolled for shiny " + rollAmount + " times. Rolled " + shinyAmount + " shinies!"), false);
        return 1;
    }
}
