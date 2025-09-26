package net.valdora.shops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ShopManager {
    private static final String SHOP_CONFIG_PATH = "config/valdora/shops/";
    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<String, ConfigShop> SHOPS = new HashMap<>();

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenShopS2CPayload.ID, OpenShopS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PurchaseC2SPayload.ID, PurchaseC2SPayload.CODEC);

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            OpenShopCommand.register(dispatcher);
            AddPokedollarsCommand.register(dispatcher);
        }));

        ServerPlayNetworking.registerGlobalReceiver(PurchaseC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                ConfigShop shop = ShopManager.getShopById(payload.shopId());
                if (shop == null) return;

                ShopItem item = shop.items.stream()
                        .filter(it -> payload.itemId().equals(it.item))
                        .findFirst()
                        .orElse(null);
                if (item == null) return;

                PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
                if (progress == null) {
                    Valdora.LOGGER.warn(player.getName() + " has no or invalid save data!");
                    return;
                }

                int totalCost = item.cost * payload.amount();
                if (!progress.hasEnoughPokedollars(totalCost)) {
                    player.sendMessage(Text.literal("You dont have enough pokedollars to buy this!"));
                    return;
                }

                progress.subtractPokedollars(totalCost);
                player.giveItemStack(item.getItem(payload.amount()));
                PlayerSaveDataManager.INSTANCE.saveProgress(player.getServer(), player.getUuid());

                player.sendMessage(Text.literal("You bought " + payload.amount() + "x " + item.getItem(1).getName().getString() + " for " + (item.cost * payload.amount())), false);
            });
        });

        load();
    }

    public static void load() {
        SHOPS.clear();

        try {
            Path configPath = Paths.get(SHOP_CONFIG_PATH);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                Valdora.LOGGER.info("Created shops config directory");
                return;
            }

            Files.walk(configPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try (FileReader reader = new FileReader(path.toFile())) {
                            ConfigShop shop = GSON.fromJson(reader, ConfigShop.class);
                            if (shop != null && shop.id != null) {
                                SHOPS.put(shop.id, shop);
                            } else {
                                Valdora.LOGGER.error("Invalid shop config in file: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            Valdora.LOGGER.error("Failed to read shop config from " + path.getFileName() + ": " + e.getMessage());
                        } catch (Exception e) {
                            Valdora.LOGGER.error("Error parsing shop config from " + path.getFileName() + ": " + e.getMessage());
                        }
                    });

            Valdora.LOGGER.info("Successfully registered " + SHOPS.size() + " shops");

        } catch (IOException e) {
            Valdora.LOGGER.error("Failed to access shops config directory: " + e.getMessage());
        }
    }

    public static Map<String, ConfigShop> getShops() {
        return SHOPS;
    }

    public static ConfigShop getShopById(String id) {
        if (SHOPS.containsKey(id)) {
            return SHOPS.get(id);
        }
        return null;
    }
}
