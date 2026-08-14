package net.valdora.areanotifications;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public class ClientAreaNotificationHandler {
    private static final Identifier NOTIF_BG = Identifier.of(Valdora.MOD_ID, "textures/gui/area_entry_panel.png");
    private static final long FADE_IN_MS = 500L;
    private static final long STAY_MS = 3000L;
    private static final long FADE_OUT_MS = 500L;
    private static final long TOTAL_MS = FADE_IN_MS + STAY_MS + FADE_OUT_MS;
    
    private static String notifMessage = null;
    private static long notifStartTime = 0L;
    
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AreaNotificationPayload.ID, (payload, context) -> {
            notifMessage = payload.message();
            notifStartTime = System.currentTimeMillis();
        });
        
        HudRenderCallback.EVENT.register(ClientAreaNotificationHandler::renderNotification);
    }
    
    private static void renderNotification(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (notifMessage == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        long elapsed = System.currentTimeMillis() - notifStartTime;
        
        if (elapsed > TOTAL_MS) {
            notifMessage = null;
            return;
        }
        
        if (elapsed < 1L) return;
        
        float alphaFraction;
        if (elapsed < FADE_IN_MS) {
            float progress = (float) elapsed / FADE_IN_MS;
            alphaFraction = (float) Math.pow(progress, 4f);
        } else if (elapsed < FADE_IN_MS + STAY_MS) {
            alphaFraction = 1f;
        } else {
            float progress = (float) (elapsed - (FADE_IN_MS + STAY_MS)) / FADE_OUT_MS;
            alphaFraction = 1f - (float) Math.pow(progress, 2f);
        }
        alphaFraction = Math.max(0f, Math.min(1f, alphaFraction));
        if (alphaFraction < 0.05f) return;
        int alpha = (int) (alphaFraction * 255);
        
        int scaledWidth = client.getWindow().getScaledWidth();
        int bgWidth = Math.min(200, Math.max(100, (int) (scaledWidth * 0.8f)));
        int bgHeight = bgWidth / 4;
        
        int x = (scaledWidth - bgWidth) / 2;
        int y = 10;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        drawContext.setShaderColor(1f, 1f, 1f, alphaFraction);
        drawContext.drawTexture(NOTIF_BG, x, y, 0, 0, bgWidth, bgHeight, 200, 50);
        drawContext.setShaderColor(1f, 1f, 1f, 1f);
        
        RenderSystem.disableBlend();
        
        Text text = Text.literal(notifMessage);
        int textWidth = client.textRenderer.getWidth(text);
        int textX = x + (bgWidth - textWidth) / 2;
        int textY = y + (bgHeight - client.textRenderer.fontHeight) / 2;
        
        int shadowColor = 0x404040 | (alpha << 24);
        drawContext.drawText(client.textRenderer, text, textX + 1, textY + 1, shadowColor, false);
        
        int mainColor = 0xFFFFFF | (alpha << 24);
        drawContext.drawText(client.textRenderer, text, textX, textY, mainColor, false);
    }
}
