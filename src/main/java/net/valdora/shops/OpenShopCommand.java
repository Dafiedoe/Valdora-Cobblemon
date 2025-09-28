package net.valdora.shops;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.Valdora;

public class OpenShopCommand {
    private static final SuggestionProvider<ServerCommandSource> SHOP_ID_SUGGESTER =
            (context, builder) -> {
                for (String id : ShopManager.getShops().keySet()) {
                    builder.suggest(id);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora")
                .requires(src -> Permissions.check(src, "valdora.openshop"))
                .then(CommandManager.literal("openshop")
                        .then(CommandManager.argument("shopid", StringArgumentType.word())
                                .suggests(SHOP_ID_SUGGESTER)
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "shopid")))))));
    }

    private static int execute(CommandContext<ServerCommandSource> context, String shopId) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");

        ConfigShop shop = ShopManager.getShopById(shopId);
        if (shop == null) {
            Valdora.LOGGER.warn("Shop: " + shop.id + " not found!");
            return 0;
        }

        OpenShopS2CPayload payload = new OpenShopS2CPayload(shop, player);
        ServerPlayNetworking.send(player, payload);
        return 1;
    }
}
