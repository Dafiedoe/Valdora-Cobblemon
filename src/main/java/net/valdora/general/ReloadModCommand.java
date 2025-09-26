package net.valdora.general;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.valdora.Valdora;
import net.valdora.savedata.checkpoints.CheckPointManager;
import net.valdora.shops.ShopManager;
import net.valdora.spawning.SpawnPoolManager;
import net.valdora.trainers.TrainerManager;

public class ReloadModCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("reload")
                        .executes(ReloadModCommand::execute)));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        Valdora.loadConfig();
        SpawnPoolManager.load();
        TrainerManager.load();
        CheckPointManager.load();
        ShopManager.load();

        context.getSource().sendMessage(Text.literal("Valdora has been reloaded!"));

        return 1;
    }
}
