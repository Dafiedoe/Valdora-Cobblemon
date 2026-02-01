package net.valdora.pokephone.appscreens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

public class ProfileAppScreen extends Screen {
    private final MinecraftClient client = MinecraftClient.getInstance();

    public ProfileAppScreen(Text title) {
        super(title);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        if (client.player == null) return;

        renderPlayerModel(context, 40, this.height / 20 + 50, 80);

        int infoX = this.width / 2 - 50;
        int infoY = this.height / 2 - 60;

        context.drawTextWithShadow(this.textRenderer, Text.literal("Player Name: " + client.player.getName().getString()), infoX, infoY, 0xFFFFFF);
        infoY += 20;


    }

    private void renderPlayerModel(DrawContext context, int x, int y, int size) {
        if (client == null || client.player == null) return;

        PlayerEntity player = client.player;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 50);
        matrices.scale(size, -size, size);
        matrices.translate(0, -1.5, 0);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));

        dispatcher.render(player, 0, 0, 0, 0, 1, matrices, context.getVertexConsumers(), 0xF000F0);
        context.draw();
        matrices.pop();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
