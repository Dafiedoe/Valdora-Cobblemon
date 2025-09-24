package net.valdora.shops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.valdora.Valdora;

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

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            OpenShopCommand.register(dispatcher);
        }));

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
