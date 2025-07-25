package net.valdora.trainers.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.valdora.trainers.TrainerManager;

public class ReloadTrainersCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("reloadtrainers").requires(source -> source.hasPermissionLevel(2)).executes(ReloadTrainersCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        TrainerManager.load();

        return 1;
    }
}
