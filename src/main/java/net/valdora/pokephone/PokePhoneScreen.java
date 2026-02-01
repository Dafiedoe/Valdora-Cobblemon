package net.valdora.pokephone;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.pokephone.apps.ProfileApp;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class PokePhoneScreen extends Screen {
    private static final int ICON_SIZE = 32;
    private static final int BUTTON_HEIGHT = ICON_SIZE + 20;
    private static final int BUTTON_WIDTH = 48;
    private static final int COLUMNS = 4;
    private static final int SPACING = 10;

    private final List<App> apps = new ArrayList<>();

    public PokePhoneScreen() {
        super(Text.literal("PokePhone"));
        apps.add(new ProfileApp());
    }

    @Override
    protected void init() {
        int startX = (this.width - (COLUMNS * BUTTON_WIDTH + (COLUMNS - 1) * SPACING)) / 2;
        int startY = (this.height - ((apps.size() / COLUMNS + 1) * BUTTON_HEIGHT)) / 2;

        for (int i = 0; i < apps.size(); i++) {
            App app = apps.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + col * (BUTTON_WIDTH + SPACING);
            int y = startY + row * (BUTTON_HEIGHT + SPACING);

            this.addDrawableChild(new AppButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, app, button -> {
                app.onOpen(MinecraftClient.getInstance().player);
            }));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(Identifier.of(Valdora.MOD_ID, "textures/gui/phone_background.png"), 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
