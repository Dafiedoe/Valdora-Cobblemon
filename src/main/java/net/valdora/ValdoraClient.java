package net.valdora;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import static net.valdora.general.ModEntities.SURF_BOARD;

public class ValdoraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                SURF_BOARD,
                context -> new BoatEntityRenderer(context, false) {
                    @Override
                    public Identifier getTexture(BoatEntity entity) {
                        return Identifier.of("minecraft", "textures/entity/boat/oak.png");
                    }
                }
        );
    }
}
