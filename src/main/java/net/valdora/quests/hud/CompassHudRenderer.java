package net.valdora.quests.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.valdora.Valdora;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class CompassHudRenderer implements HudRenderCallback {
    private static final int COMPASS_SIZE = 20;
    private static final int MARGIN = 15;
    private static final Identifier COMPASS_TEXTURE = Identifier.of(Valdora.MOD_ID, "textures/gui/reach_quest_compass.png");

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !CompassHudClient.shouldShowCompass()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int hotbarY = screenHeight - 22;
        int x = screenWidth / 2 - COMPASS_SIZE / 2;
        int y = hotbarY - COMPASS_SIZE - MARGIN;

        drawContext.fill(x - 3, y - 3, x + COMPASS_SIZE + 3, y + COMPASS_SIZE + 3, 0x80000000);

        float yawToTarget = CompassHudClient.getYawToTarget(client);

        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(x + COMPASS_SIZE / 2f, y + COMPASS_SIZE / 2f, 0);
        matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(yawToTarget)));
        matrices.translate(-COMPASS_SIZE / 2f, -COMPASS_SIZE / 2f, 0);
        drawContext.drawTexture(COMPASS_TEXTURE, 0, 0, 0, 0, COMPASS_SIZE, COMPASS_SIZE, COMPASS_SIZE, COMPASS_SIZE);
        matrices.pop();

        Vec3d playerPos = client.player.getPos();
        double distance = playerPos.distanceTo(CompassHudClient.getTargetPos());
        Text distText = Text.literal(String.format("%.0fm", distance));
        int distWidth = client.textRenderer.getWidth(distText);
        int textY = y + COMPASS_SIZE + 4;
        drawContext.drawText(client.textRenderer, distText, x + (COMPASS_SIZE - distWidth) / 2, textY, 0xFFFFFF, true);
    }
}