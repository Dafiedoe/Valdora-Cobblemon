package net.valdora;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.valdora.general.ModBlockEntities;
import net.valdora.general.ModBlocks;
import net.valdora.savedata.PlayerSaveDataManager;
import net.valdora.savedata.flaggedbarrier.FlaggedBarrierEntityRenderer;
import net.valdora.savedata.profiles.ProfileCreateScreen;
import net.valdora.savedata.profiles.ProfileScreen;

import static net.valdora.general.ModEntities.SURF_BOARD;

public class ValdoraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register surf board entity renderer
        EntityRendererRegistry.register(
                SURF_BOARD,
                context -> new BoatEntityRenderer(context, false) {
                    @Override
                    public Identifier getTexture(BoatEntity entity) {
                        return Identifier.of("minecraft", "textures/entity/boat/oak.png");
                    }
                }
        );

        // Register block rendering
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

        // Handle profile creation results
        ClientPlayNetworking.registerGlobalReceiver(PlayerSaveDataManager.ProfileCreationResultPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                if (client.currentScreen instanceof ProfileCreateScreen screen) {
                    if (!payload.success()) {
                        screen.setErrorMessage(payload.errorMessage());
                    } else {
                        client.setScreen(screen.parent); // Return to parent screen on success
                    }
                }
            });
        });
    }
}
