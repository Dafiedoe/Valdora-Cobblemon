package net.valdora.general;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.valdora.Valdora;
import net.valdora.areanotifications.AreaNotificationManager;
import net.valdora.quests.QuestManager;
import net.valdora.savedata.checkpoints.CheckPointManager;
import net.valdora.shops.ShopManager;
import net.valdora.spawning.SpawnPoolManager;
import net.valdora.trainers.TrainerManager;
import net.valdora.warps.WarpManager;

public class ReloadModCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora").then(CommandManager.literal("reload").requires(source -> Permissions.check(source, "valdora.reload", 2))
                .executes(ReloadModCommand::execute)));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        Valdora.loadConfig();
        SpawnPoolManager.load();
        TrainerManager.load();
        QuestManager.load();
        CheckPointManager.load();
        ShopManager.load();
        WarpManager.load();
        AreaNotificationManager.load();
        
        Valdora.reloadPlayerQuestHud();
        
        context.getSource().sendMessage(Text.literal("Valdora has been reloaded!"));
        
        return 1;
    }
}
