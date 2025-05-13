package net.valdora.spawning.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.valdora.spawning.SpawnPoolManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ReloadSpawnPoolsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("reloadspawnpools").requires(source -> source.hasPermissionLevel(2)).executes(ReloadSpawnPoolsCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        SpawnPoolManager.load();

        return 1;
    }
}
