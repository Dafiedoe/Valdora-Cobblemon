package net.valdora;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.valdora.areanotifications.ClientAreaNotificationHandler;
import net.valdora.general.ModBlockEntities;
import net.valdora.general.ModBlocks;
import net.valdora.general.ModEntities;
import net.valdora.pokephone.PokePhoneRespondProgressPayload;
import net.valdora.pokephone.appscreens.ProfileAppScreen;
import net.valdora.quests.hud.CompassHudClient;
import net.valdora.quests.hud.CompassHudRenderer;
import net.valdora.quests.hud.QuestHudClient;
import net.valdora.quests.hud.QuestHudRenderer;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.savedata.flaggedbarrier.ClientPlayerFlagCache;
import net.valdora.savedata.flaggedbarrier.FlaggedBarrierEntityRenderer;
import net.valdora.savedata.flaggedbarrier.PlayerFlagsS2CPayload;
import net.valdora.savedata.profiles.ProfileCreateScreen;
import net.valdora.savedata.profiles.ProfileScreen;
import net.valdora.shops.ConfigShop;
import net.valdora.shops.OpenShopS2CPayload;
import net.valdora.shops.ShopScreen;
import net.valdora.trainers.DummyModel;
import net.valdora.trainers.DummyRenderer;

import static net.valdora.general.ModEntities.SURF_BOARD;

public class ValdoraClient implements ClientModInitializer {
    public static final EntityModelLayer DUMMY_LAYER = new EntityModelLayer(Identifier.of(Valdora.MOD_ID, "dummy"), "main");
    
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(SURF_BOARD, context -> new BoatEntityRenderer(context, false) {
                    @Override
                    public Identifier getTexture(BoatEntity entity) {
                        return Identifier.of("minecraft", "textures/entity/boat/oak.png");
                    }
                }
        );
        
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLAGGED_BARRIER, RenderLayer.getTranslucent());
        BlockEntityRendererRegistry.register(ModBlockEntities.FLAGGED_BARRIER_ENTITY, FlaggedBarrierEntityRenderer::new);
        
        ClientPlayNetworking.registerGlobalReceiver(PlayerSaveDataManager.OpenProfileGuiPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                if (client.currentScreen instanceof ProfileScreen screen) {
                    screen.updateProfiles(payload.profiles());
                } else {
                    client.setScreen(new ProfileScreen(net.minecraft.text.Text.literal("Profile Selection"), payload.profiles()));
                }
            });
        });
        
        ClientPlayNetworking.registerGlobalReceiver(PlayerSaveDataManager.ProfileCreationResultPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                if (client.currentScreen instanceof ProfileCreateScreen screen) {
                    if (!payload.success()) {
                        screen.setErrorMessage(payload.errorMessage());
                    } else {
                        client.setScreen(screen.parent);
                    }
                }
            });
        });
        
        ClientPlayNetworking.registerGlobalReceiver(PlayerFlagsS2CPayload.PAYLOAD_ID, (PlayerFlagsS2CPayload payload, ClientPlayNetworking.Context ctx) -> {
                    MinecraftClient client = ctx.client();
                    ClientPlayerFlagCache.setFlags(client.player.getUuid(), payload.flags());
                }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(OpenShopS2CPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                ConfigShop shop = payload.toConfigShop();
                ShopScreen screen = new ShopScreen(shop);
                screen.setPlayerPokedollars(payload.pokedollars);
                client.setScreen(screen);
            });
        });
        
        ClientPlayNetworking.registerGlobalReceiver(PokePhoneRespondProgressPayload.ID, (payload, context) -> {
            int badgeCount = payload.badgeCount();
            int pokedollars = payload.pokedollars();
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof ProfileAppScreen profileScreen) {
                    MinecraftClient.getInstance().setScreen(new ProfileAppScreen(Text.literal("Your Profile"), badgeCount, pokedollars));
                }
            });
        });
        
        EntityRendererRegistry.register(ModEntities.DUMMY_ENTITY, DummyRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(DUMMY_LAYER, DummyModel::getTexturedModelData);
        
        ClientAreaNotificationHandler.register();
        
        QuestHudClient.register();
        HudRenderCallback.EVENT.register(new QuestHudRenderer());
        CompassHudClient.register();
        HudRenderCallback.EVENT.register(new CompassHudRenderer());
    }
}
