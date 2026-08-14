package net.valdora.pokephone.appscreens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.valdora.pokephone.PokePhoneRequestProgressPayload;

@Environment(EnvType.CLIENT)
public class ProfileAppScreen extends Screen {
    private final MinecraftClient client = MinecraftClient.getInstance();
    
    private int badgeCount = 0;
    private int pokedollars = 0;
    private boolean dataLoaded = false;
    
    public ProfileAppScreen(Text title) {
        super(title);
        
        ClientPlayNetworking.send(new PokePhoneRequestProgressPayload(client.player.getUuid().toString()));
        dataLoaded = false;
    }
    
    public ProfileAppScreen(Text title, int badgeCount, int pokedollars) {
        super(title);
        this.badgeCount = badgeCount;
        this.pokedollars = pokedollars;
        
        dataLoaded = true;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        
        if (client.player == null) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }
        
        if (!dataLoaded) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Loading profile..."), this.width / 2, this.height / 2, 0xFFFFFFFF);
            super.render(context, mouseX, mouseY, delta);
            return;
        }
        
        renderPlayerModel(context, this.width / 2 - 150, this.height / 2 - 40, 80);
        
        int infoX = this.width / 2 - 50;
        int infoY = this.height / 2 - 60;
        
        context.drawTextWithShadow(this.textRenderer, Text.literal("Player Name: " + client.player.getName().getString()), infoX, infoY, 0xFFFFFF);
        infoY += 20;
        
        context.drawTextWithShadow(this.textRenderer, Text.literal("Gym Badges: " + badgeCount + "/8"), infoX, infoY, 0xFFFFFF);
        infoY += 20;
        
        context.drawTextWithShadow(this.textRenderer, Text.literal("Pokedollars: ₽" + pokedollars), infoX, infoY, 0xFFFFFF);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int color = 0xFFFF0000;
        int halfLength = 10;
        
        context.fill(centerX - halfLength, centerY, centerX + halfLength + 1, centerY + 1, color);
        context.fill(centerX, centerY - halfLength, centerX + 1, centerY + halfLength + 1, color);
    }
    
    private void renderPlayerModel(DrawContext context, int x, int y, int size) {
        if (client.player == null) return;
        
        PlayerEntity player = client.player;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        var matrices = context.getMatrices();
        
        float bodyYaw = player.bodyYaw;
        float headYaw = player.headYaw;
        float prevBodyYaw = player.prevBodyYaw;
        float prevHeadYaw = player.prevHeadYaw;
        float pitch = player.getPitch();
        float prevPitch = player.prevPitch;
        
        player.bodyYaw = 0.0F;
        player.headYaw = 0.0F;
        player.prevBodyYaw = 0.0F;
        player.prevHeadYaw = 0.0F;
        player.setPitch(0.0F);
        player.prevPitch = 0.0F;
        
        matrices.push();
        matrices.translate(x + 0.5, y + 1.5, 50);
        matrices.scale(size, -size, size);
        
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F));
        
        dispatcher.render(player, 0, 0, 0, 0, 1.0F, matrices, context.getVertexConsumers(), 0xF000F0);
        context.draw();
        matrices.pop();
        
        player.bodyYaw = bodyYaw;
        player.headYaw = headYaw;
        player.prevBodyYaw = prevBodyYaw;
        player.prevHeadYaw = prevHeadYaw;
        player.setPitch(pitch);
        player.prevPitch = prevPitch;
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}