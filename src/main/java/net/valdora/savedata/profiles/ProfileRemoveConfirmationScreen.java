package net.valdora.savedata.profiles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ProfileRemoveConfirmationScreen extends Screen {
    private final String profileName;
    private final ProfileScreen parentScreen;

    public ProfileRemoveConfirmationScreen(Text title, String profileName, ProfileScreen parentScreen) {
        super(title);
        this.profileName = profileName;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Yes"), button -> {
            client.getNetworkHandler().sendPacket(
                    ClientPlayNetworking.createC2SPacket(new net.valdora.savedata.PlayerSaveDataManager.DeleteProfilePayload(profileName))
            );
            client.setScreen(parentScreen);
        }).dimensions(centerX - 50, centerY + 10, 40, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("No"), button -> {
            client.setScreen(parentScreen);
        }).dimensions(centerX + 10, centerY + 10, 40, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);

        int centerX = width / 2;
        int centerY = height / 2;
        String confirmText = "Are you sure you want to delete '" + profileName + "'?\nThis action cannot be reverted.";
        context.drawText(textRenderer, Text.literal(confirmText), centerX - textRenderer.getWidth(confirmText) / 2, centerY - 10, 0xFFFFFF, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
    }
}