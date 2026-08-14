package net.valdora.pokephone;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AppButton extends ButtonWidget {
    private final App app;
    private static final int ICON_SIZE = 32;
    
    public AppButton(int x, int y, int width, int height, App app, PressAction onPress) {
        super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.app = app;
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.isHovered()) {
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x40FFFFFF);
        }
        
        int iconX = this.getX() + (this.getWidth() - ICON_SIZE) / 2;
        int iconY = this.getY() + 4;
        context.drawTexture(app.getIcon(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        
        int textY = iconY + ICON_SIZE + 4;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, app.getName(), this.getX() + this.getWidth() / 2, textY, 0xFFFFFF);
    }
}
