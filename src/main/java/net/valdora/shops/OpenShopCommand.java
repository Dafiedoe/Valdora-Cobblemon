package net.valdora.shops;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class OpenShopCommand {
    // SuggestionProvider that suggests every shop id currently registered in ShopManager
    private static final SuggestionProvider<ServerCommandSource> SHOP_ID_SUGGESTER =
            (context, builder) -> {
                // Suggest from the current registry; this makes suggestions dynamic.
                for (String id : ShopManager.getShops().keySet()) {
                    builder.suggest(id);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("valdora")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("openshop")
                        .then(CommandManager.argument("shopid", StringArgumentType.word())
                                .suggests(SHOP_ID_SUGGESTER)
                                .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "shopid"))))));
    }

    private static int execute(CommandContext<ServerCommandSource> context, String shopId) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();

        ConfigShop shop = ShopManager.getShopById(shopId);
        if (shop == null) {
            source.sendFeedback(() -> Text.literal("Shop not found: " + shopId), false);
            return 0;
        }

        // create typed payload (packet-object) and send it
        OpenShopS2CPayload payload = new OpenShopS2CPayload(shop);
        ServerPlayNetworking.send(player, payload); // uses send(player, T packet) overload in packet-object API

        source.sendFeedback(() -> Text.literal("Opened shop '" + shopId + "' for " + player.getName().getString()), true);
        return 1;
    }
}
