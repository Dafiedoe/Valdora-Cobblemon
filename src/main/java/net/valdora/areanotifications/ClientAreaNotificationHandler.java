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

        // Early skip for very initial frames to prevent any potential full-opacity render
        if (elapsed < 1L) return;

        // Calculate alpha with higher-power easing for ultra-slow fade-in start
        float alphaFraction;
        if (elapsed < FADE_IN_MS) {
            float progress = (float) elapsed / FADE_IN_MS;
            // Quartic ease-in: even slower initial ramp-up to completely avoid flash
            alphaFraction = (float) Math.pow(progress, 4f);
        } else if (elapsed < FADE_IN_MS + STAY_MS) {
            alphaFraction = 1f;
        } else {
            float progress = (float) (elapsed - (FADE_IN_MS + STAY_MS)) / FADE_OUT_MS;
            // Quadratic ease-out for fade-out
            alphaFraction = 1f - (float) Math.pow(progress, 2f);
        }
        // Clamp to [0,1] for safety, and skip render if too faint
        alphaFraction = Math.max(0f, Math.min(1f, alphaFraction));
        if (alphaFraction < 0.05f) return;  // Increased threshold to skip more initial faint frames
        int alpha = (int) (alphaFraction * 255);

        // Proportional sizing
        int scaledWidth = client.getWindow().getScaledWidth();
        int bgWidth = Math.min(200, Math.max(100, (int) (scaledWidth * 0.8f)));
        int bgHeight = bgWidth / 4;  // Assuming 4:1 aspect ratio

        // Position: Top-center
        int x = (scaledWidth - bgWidth) / 2;
        int y = 10;

        // Enable blending for texture alpha support
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawContext.setShaderColor(1f, 1f, 1f, alphaFraction);
        drawContext.drawTexture(NOTIF_BG, x, y, 0, 0, bgWidth, bgHeight, 200, 50);
        drawContext.setShaderColor(1f, 1f, 1f, 1f);

        // Disable blending to avoid affecting other HUD elements
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