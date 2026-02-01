package net.valdora.pokephone;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;

public class PokePhoneInitializer {
    public static void register() {
        Registry.register(Registries.ITEM, Identifier.of(Valdora.MOD_ID, "pokephone"), new PokePhoneItem(new Item.Settings().maxCount(1)));

        ServerPlayNetworking.registerGlobalReceiver(PokePhoneRequestProgressPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
            if (progress == null) {
                Valdora.LOGGER.error("Player '" + player.getName().getString() + "' has no or invalid save data");
                return;
            }

            if (progress.getFlags().containsKey("badges")) {
                String badgesStr = progress.getFlags().get("badges");
                int badges = 0;
                try {
                    badges = Integer.parseInt(badgesStr);
                } catch (NumberFormatException e) {
                    Valdora.LOGGER.error("Player '" + player.getName().getString() + "' badges flag is not a valid number!");
                    return;
                }

                ServerPlayNetworking.send(player, new PokePhoneRespondProgressPayload(badges, progress.getPokedollars()));
            }
        });
    }
}
