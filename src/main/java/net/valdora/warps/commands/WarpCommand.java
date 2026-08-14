package net.valdora.warps.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.warps.WarpManager;

public class WarpCommand {
    private static final SuggestionProvider<ServerCommandSource> WARP_SUGGESTIONS = (context, builder) -> {
        WarpManager.getWarps().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("warp")
                .requires(source -> Permissions.check(source, "valdora.warp", 2))
                .then(CommandManager.argument("warpId", StringArgumentType.string())
                        .suggests(WARP_SUGGESTIONS)
                        .executes(WarpCommand::execute)));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = context.getSource().getPlayerOrThrow();
        String warpId = StringArgumentType.getString(context, "warpId");
        
        WarpManager.warpPlayer(targetPlayer, warpId);
        return 1;
    }
}
