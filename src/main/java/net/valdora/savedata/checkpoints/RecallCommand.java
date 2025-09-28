package net.valdora.savedata.checkpoints;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.Valdora;

public class RecallCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora")
                .then(CommandManager.literal("recall")
                        .requires(source -> Permissions.check(source, "valdora.recall", 2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(RecallCommand::execute))));
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");

        CheckPointManager.recallPlayerToCheckPoint(player, false);

        return 1;
    }
}
