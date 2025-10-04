package net.valdora.quests.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class QuestHudRenderer implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !QuestHudClient.hasActiveQuest()) {
            return;
        }

        List<Text> lines = QuestHudClient.getQuestDisplayLines();
        int screenWidth = client.getWindow().getScaledWidth();
        int lineHeight = client.textRenderer.fontHeight;
        int rightMargin = 10;  // Right margin set to 50px

        // Top margin
        int y = 60;

        // Calculate max width for right-alignment
        int maxTextWidth = lines.stream().mapToInt(line -> client.textRenderer.getWidth(line)).max().orElse(0);
        int x = screenWidth - maxTextWidth - rightMargin;  // Right-align: screenWidth - width - margin

        // Draw background semi-transparent rect (covers all lines)
        int totalHeight = lines.size() * lineHeight;
        drawContext.fill(x - 5, y - 5, x + maxTextWidth + 5, y + totalHeight + 5, 0x80000000);  // Black semi-transparent

        // Draw each line (right-aligned)
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            int lineWidth = client.textRenderer.getWidth(line);
            int lineX = screenWidth - lineWidth - rightMargin;  // Per-line right-align if widths vary
            drawContext.drawText(client.textRenderer, line, lineX, y + (i * lineHeight), 0xFFFFFF, true);  // White text, shadow=true
        }
    }
}