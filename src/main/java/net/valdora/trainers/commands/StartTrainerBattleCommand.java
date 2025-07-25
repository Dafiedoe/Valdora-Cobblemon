package net.valdora.trainers.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
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
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                CommandManager.argument("trainerId", StringArgumentType.string())
                                        .suggests(TRAINER_SUGGESTIONS)
                                        .executes(StartTrainerBattleCommand::execute)
                        )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String trainerId = StringArgumentType.getString(context, "trainerId");
        TrainerConfig trainer = TrainerManager.getTrainerById(trainerId);

        if (trainer == null) {
            context.getSource().sendError(Text.literal("No trainer found with ID: " + trainerId));
            Valdora.LOGGER.error("No trainer found with ID: " + trainerId);
            return 0;
        }

        PokemonUtils.startTrainerBattle(player, trainerId);
        return 1;
    }
}
