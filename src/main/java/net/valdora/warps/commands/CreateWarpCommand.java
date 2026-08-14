package net.valdora.warps.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.warps.WarpManager;

public class CreateWarpCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("valdora")
                        .then(CommandManager.literal("createwarp")
                                .requires(source -> Permissions.check(source, "valdora.createwarp", 2))
                                .then(CommandManager.argument("warpId", StringArgumentType.string())
                                        .executes(CreateWarpCommand::execute))));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = context.getSource().getPlayerOrThrow();
        String warpId = StringArgumentType.getString(context, "warpId");
        
        WarpManager.createWarp(targetPlayer, warpId);
        return 1;
    }
}
