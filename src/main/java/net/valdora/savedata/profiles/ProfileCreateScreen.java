package net.valdora.savedata.profiles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ProfileCreateScreen extends Screen {
    public final Screen parent;
    private TextFieldWidget profileNameField;
    private String errorMessage = null;
    private int errorTimer = 0;
    
    public ProfileCreateScreen(Screen parent) {
        super(Text.literal("Create Profile"));
        this.parent = parent;
    }
    
    public void setErrorMessage(String message) {
        this.errorMessage = message;
        this.errorTimer = 100;
    }
    
    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        
        profileNameField = new TextFieldWidget(textRenderer, centerX - 80, centerY - 20, 160, 20, Text.literal("Profile Name"));
        profileNameField.setMaxLength(16);
        addDrawableChild(profileNameField);
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Create"), button -> {
            String name = profileNameField.getText().trim();
            if (name.isEmpty()) {
                setErrorMessage("Name cannot be empty!");
                return;
            }
            ClientPlayNetworking.send(new net.valdora.savedata.PlayerSaveDataManager.CreateProfilePayload(name));
        }).dimensions(centerX - 80, centerY + 10, 75, 20).build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> client.setScreen(parent)).dimensions(centerX + 5, centerY + 10, 75, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, Text.literal("Enter Profile Name"), width / 2 - 50, height / 2 - 50, 0xFFFFFF, true);
        super.render(context, mouseX, mouseY, delta);
        
        if (errorMessage != null && errorTimer > 0) {
            context.drawText(textRenderer, Text.literal(errorMessage), width / 2 - textRenderer.getWidth(errorMessage) / 2, height / 2 + 40, 0xFF0000, true);
            errorTimer--;
            if (errorTimer <= 0) errorMessage = null;
        }
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
