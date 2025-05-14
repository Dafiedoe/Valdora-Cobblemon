package net.valdora.npc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.text.Text;

import net.valdora.npc.ModNPC;
import net.valdora.npc.custom.StaticNPC;

import java.util.concurrent.CompletableFuture;
import java.util.List;

public class SpawnNPCCommand {

    private static final List<String> NPC_TYPES = List.of(
            "static_npc"
    );

    private static final SuggestionProvider<ServerCommandSource> NPC_TYPE_SUGGESTER =
            (CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) ->
                    CommandSource.suggestMatching(NPC_TYPES, builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("spawnvaldoranpc")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(withTypeArgument())
        );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> withTypeArgument() {
        return CommandManager.argument("type", StringArgumentType.word())
                .suggests(NPC_TYPE_SUGGESTER)
                .executes(ctx -> execute(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "type")
                ));
    }

    private static int execute(ServerCommandSource src, String type) {
        World world = src.getWorld();
        BlockPos pos = src.getPlayer().getBlockPos().up();

        switch (type) {
            case "static_npc" -> {
                StaticNPC npc = ModNPC.STATIC_NPC.create(world);
                npc.refreshPositionAndAngles(
                        pos.getX() + 0.5,
                        pos.getY() - 1,
                        pos.getZ() + 0.5,
                        0, 0
                );
                world.spawnEntity(npc);
                src.sendFeedback(() -> Text.literal("Spawned Static NPC"), false);
            }
            default -> {
                src.sendError(Text.literal("Unknown NPC type: " + type));
            }
        }

        return 1;
    }
}
