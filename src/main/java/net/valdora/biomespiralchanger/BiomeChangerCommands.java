package net.valdora.biomespiralchanger;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class BiomeChangerCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("valdora")
                    .then(CommandManager.literal("biomechanger")
                            .requires(source -> Permissions.check(source, "valdora.biomechanger", 2))
                            .then(CommandManager.literal("setup")
                                    .then(CommandManager.argument("biome", StringArgumentType.string())
                                            .executes(context -> {
                                                ServerCommandSource source = context.getSource();
                                                ServerPlayerEntity player = source.getPlayer();
                                                if (player == null) {
                                                    return 0;
                                                }
                                                
                                                BiomeChanger.Setup(player, StringArgumentType.getString(context, "biome"));
                                                return 1;
                                            })))
                            .then(CommandManager.literal("start")
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        ServerPlayerEntity player = source.getPlayer();
                                        if (player == null) {
                                            return 0;
                                        }
                                        BiomeChanger.Start(player);
                                        return 1;
                                    }))
                            .then(CommandManager.literal("stop")
                                    .executes(context -> {
                                        BiomeChanger.Stop();
                                        return 1;
                                    }))
                            .then(CommandManager.literal("pause")
                                    .executes(context -> {
                                        BiomeChanger.Pause();
                                        return 1;
                                    }))
                            .then(CommandManager.literal("resume")
                                    .executes(context -> {
                                        BiomeChanger.Resume();
                                        return 1;
                                    }))));
        });
    }
}
