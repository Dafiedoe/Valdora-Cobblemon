package net.valdora;

import net.valdora.npc.ModNPC;
import net.valdora.npc.custom.StaticNPCRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ValdoraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModNPC.STATIC_NPC, StaticNPCRenderer::new);
    }
}
