package net.valdora.npc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.valdora.npc.custom.StaticNPC;

import java.util.Comparator;
import java.util.List;

public class DeleteNPCCommand {

    private static final List<String> NPC_TYPES = List.of("static_npc");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("deletevaldoranpc")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) ->
                                        CommandSource.suggestMatching(NPC_TYPES, builder))
                                .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(1.0))
                                        .executes(ctx -> execute(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "type"),
                                                DoubleArgumentType.getDouble(ctx, "radius")
                                        ))
                                )
                        )
        );
    }

    private static int execute(ServerCommandSource src, String type, double radius) {
        World world = src.getWorld();
        Vec3d origin = src.getPlayer().getPos();

        Box searchBox = new Box(
                origin.x - radius, origin.y - radius, origin.z - radius,
                origin.x + radius, origin.y + radius, origin.z + radius
        );

        List<? extends Entity> matches;
        switch (type) {
            case "static_npc":
                matches = world.getEntitiesByClass(
                        StaticNPC.class,
                        searchBox,
                        e -> true
                );
                break;
            default:
                src.sendError(Text.literal("Unknown NPC type: " + type));
                return 0;
        }

        if (matches.isEmpty()) {
            src.sendError(Text.literal(
                    "No '" + type + "' NPCs found within " + radius + " blocks."
            ));
            return 0;
        }

        Entity closest = matches.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(origin)))
                .get();

        closest.remove(RemovalReason.DISCARDED);
        src.sendFeedback(() ->
                        Text.literal("Deleted one '" + type + "' NPC at distance "
                                + String.format("%.1f", Math.sqrt(closest.squaredDistanceTo(origin)))
                                + " blocks."),
                false
        );
        return 1;
    }
}
