package net.dafiedoe.valdora.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.dafiedoe.valdora.spawning.SpawnEntry;
import net.dafiedoe.valdora.spawning.SpawnPoolManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Collectors;

public class DebugValidSpawnsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("debugvalidspawns").requires(source -> source.hasPermissionLevel(2)).executes(DebugValidSpawnsCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        List<SpawnEntry> validSpawns = SpawnPoolManager.getValidSpawnsForPlayer(player);

        if (validSpawns.isEmpty()) {
            player.sendMessage(Text.literal("No spawns for this biome/time/weather"), false);
        } else {
            String names = validSpawns.stream().map(entry -> entry.pokemon).collect(Collectors.joining(", "));
            player.sendMessage(Text.literal("Spawnable Pokemon: " + names), false);
        }

        return 1;
    }
}
