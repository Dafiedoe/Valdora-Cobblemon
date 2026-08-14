package net.valdora.trainers.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.valdora.Valdora;
import net.valdora.trainers.TrainerConfig;
import net.valdora.trainers.TrainerManager;
import net.valdora.utils.PokemonUtils;

public class StartTrainerBattleCommand {
    private static final SuggestionProvider<ServerCommandSource> TRAINER_SUGGESTIONS = (context, builder) -> {
        TrainerManager.getTrainers().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("starttrainerbattle")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .requires(source -> Permissions.check(source, "valdora.starttrainerbattle", 2))
                                .then(CommandManager.argument("trainerId", StringArgumentType.string())
                                        .suggests(TRAINER_SUGGESTIONS)
                                        .then(CommandManager.argument("trainerUuid", StringArgumentType.string())
                                                .executes(StartTrainerBattleCommand::execute)))));
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");
        String trainerId = StringArgumentType.getString(context, "trainerId");
        String trainerUuid = StringArgumentType.getString(context, "trainerUuid");
        
        TrainerConfig trainer = TrainerManager.getTrainerById(trainerId);
        if (trainer == null) {
            context.getSource().sendError(Text.literal("No trainer found with ID: " + trainerId));
            Valdora.LOGGER.error("No trainer found with ID: {}", trainerId);
            return 0;
        }
        
        PokemonUtils.startTrainerBattle(targetPlayer, trainerId, trainerUuid);
        return 1;
    }
}
