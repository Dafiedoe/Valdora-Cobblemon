package net.valdora.shops;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;

public class AddPokedollarsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora")
                .requires(src -> Permissions.check(src, "valdora.pokedollars"))
                .then(CommandManager.literal("addpokedollars")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer())
                                        .executes(AddPokedollarsCommand::execute)))));
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity source = context.getSource().getPlayerOrThrow();

        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        if (progress == null) {
            Valdora.LOGGER.error(player.getName() + " has no or invalid save data!");
            return 0;
        }

        progress.addPokedollars(amount);

        PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());

        source.sendMessage(Text.literal("Gave " + player.getName().getString() + " ₽" + amount + "!"));
        player.sendMessage(Text.literal("You've gained ₽" + amount + "!"));

        return 1;
    }
}
