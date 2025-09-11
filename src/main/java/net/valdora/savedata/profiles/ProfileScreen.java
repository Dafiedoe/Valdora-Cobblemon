package net.valdora.savedata.profiles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ProfileScreen extends Screen {
    private final List<String> profiles;
    private static final int MAX_PROFILES = 5;
    private String selectedProfile = null;
    private String errorMessage = null;
    private int errorTimer = 0;

    public ProfileScreen(Text title, List<String> profiles) {
        super(title);
        this.profiles = new ArrayList<>(profiles);
    }

    public void updateProfiles(List<String> newProfiles) {
        this.profiles.clear();
        this.profiles.addAll(newProfiles);
        clearChildren();
        init();
    }

    public void setErrorMessage(String message) {
        this.errorMessage = message;
        this.errorTimer = 100;
    }

    @Override
    protected void init() {
        int leftX = 30;
        int startY = (height - (50 + MAX_PROFILES * 22)) / 2;
        int buttonWidth = 130;

        for (int i = 0; i < Math.min(profiles.size(), MAX_PROFILES); i++) {
            String profile = profiles.get(i);
            int y = startY + 20 + i * 22;
            ButtonWidget profileButton = ButtonWidget.builder(Text.literal(profile), button -> {
                selectedProfile = profile;
            }).dimensions(leftX, y, buttonWidth, 20).build();
            addDrawableChild(profileButton);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Select"), button -> {
            if (selectedProfile != null) {
                ClientPlayNetworking.send(new net.valdora.savedata.PlayerSaveDataManager.SelectProfilePayload(selectedProfile));
            } else {
                setErrorMessage("Please select a profile");
            }
        }).dimensions(leftX, height - 70, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Create"), button -> {
            if (profiles.size() >= MAX_PROFILES) {
                setErrorMessage("Maximum profiles reached (" + MAX_PROFILES + ")");
                return;
            }
            client.setScreen(new ProfileCreateScreen(this));
        }).dimensions(leftX, height - 40, 60, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> {
            if (selectedProfile != null) {
                client.setScreen(new ProfileRemoveConfirmationScreen(Text.literal("Confirm Delete"), selectedProfile, this));
            } else {
                setErrorMessage("Please select a profile to delete");
            }
        }).dimensions(leftX + 70, height - 40, 60, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        int sidebarWidth = 180;
        context.fill(0, 0, sidebarWidth, height, 0xFF333333); // Solid dark gray
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        String titleText = "Profile Selection";
        context.drawText(
                textRenderer,
                Text.literal(titleText),
                width / 2 - textRenderer.getWidth(titleText) / 2,
                20,
                0xFFFFFF,
                true
        );

        int sidebarWidth = 180;
        String sidebarTitle = "Profiles";
        context.drawText(
                textRenderer,
                Text.literal(sidebarTitle),
                sidebarWidth / 2 - textRenderer.getWidth(sidebarTitle) / 2,
                15,
                0xFFFFFF,
                true
        );

        int modelX = width - 120;
        int modelY = height / 2 + 50;
        renderPlayerModel(context, modelX, modelY, 100);

        super.render(context, mouseX, mouseY, delta);

        if (errorMessage != null && errorTimer > 0) {
            context.drawText(textRenderer, Text.literal(errorMessage), 40, height - 95, 0xFF0000, true);
            errorTimer--;
            if (errorTimer <= 0) {
                errorMessage = null;
            }
        }
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

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));

        dispatcher.render(player, 0, 0, 0, 0, 1, matrices, context.getVertexConsumers(), 0xF000F0);
        context.draw();
        matrices.pop();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}