package net.valdora.npc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.valdora.npc.ModNPC;
import net.valdora.npc.custom.StaticNPC;

public class SpawnNPCCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("spawnstaticnpc")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .executes(ctx -> execute(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")
                                ))
                        )
        );
    }

    private static int execute(ServerCommandSource src, String type) {
        World world = src.getWorld();
        // BlockPos.up() exists in 1.21.1 – .above() does not
        BlockPos pos = src.getPlayer().getBlockPos().up();

        if ("static_npc".equals(type)) {
            StaticNPC npc = ModNPC.STATIC_NPC.create(world);
            npc.refreshPositionAndAngles(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    0, 0
            );
            world.spawnEntity(npc);
            // sendFeedback takes a Supplier<Text> now
            src.sendFeedback(() -> Text.literal("Spawned Static NPC"), false);
        } else {
            src.sendError(Text.literal("Unknown NPC type: " + type));
        }

        return 1;
    }
}
