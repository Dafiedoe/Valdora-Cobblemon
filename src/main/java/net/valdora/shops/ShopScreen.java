package net.valdora.shops;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ShopScreen extends Screen {
    private final ConfigShop shopConfig;
    private String errorMessage = null;
    private int errorTimer = 0;

    private final int padding = 20;
    private int listLeft, listTop, listWidth, listHeight, listRight;
    private final int itemHeight = 36;
    private int scroll = 0;

    public ShopScreen(ConfigShop shopItems) {
        super(Text.literal(shopItems.title == null ? "Shop" : shopItems.title));
        this.shopConfig = shopItems;
    }

    public void setErrorMessage(String message) {
        this.errorMessage = message;
        this.errorTimer = 100;
    }

    @Override
    protected void init() {
        super.init();

        listLeft = padding;
        listTop = 40;
        listRight = this.width - padding;
        listWidth = listRight - listLeft;
        listHeight = this.height - listTop - padding;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), (b) -> this.client.setScreen(null))
                .dimensions(this.width - 80, 10, 70, 20).build());
    }

    private List<ShopItem> items() {
        return shopConfig.items == null ? List.of() : shopConfig.items;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int titleX = this.width / 2 - this.textRenderer.getWidth(this.title) / 2;
        context.drawText(this.textRenderer, this.title, titleX, 10, 0xFFFFFF, false);

        context.fill(listLeft - 2, listTop - 2, listRight + 2, listTop + listHeight + 2, 0xCC000000);
        context.fill(listLeft, listTop, listRight, listTop + listHeight, 0x88000000);

        List<ShopItem> items = items();
        int contentHeight = items.size() * itemHeight;
        int maxScroll = Math.max(0, contentHeight - listHeight);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;

        int firstIndex = Math.max(0, scroll / itemHeight);
        int lastIndex = Math.min(items.size() - 1, (scroll + listHeight) / itemHeight);

        int yStart = listTop - (scroll % itemHeight);

        for (int i = firstIndex; i <= lastIndex; i++) {
            ShopItem s = items.get(i);
            int y = yStart + (i - firstIndex) * itemHeight;
            int x = listLeft;

            boolean hovered = mouseX >= listLeft && mouseX < listRight && mouseY >= y && mouseY < y + itemHeight;

            if (hovered) {
                context.fill(x, y, x + listWidth, y + itemHeight, 0x40FFFFFF);
            } else {
                if ((i & 1) == 0) context.fill(x, y, x + listWidth, y + itemHeight, 0x15000000);
            }

            ItemStack stack = s.getItem();
            if (!stack.isEmpty()) {
                context.drawItem(stack, x + 4, y + (itemHeight - 16) / 2);
                context.drawText(this.textRenderer, stack.getName(), x + 24, y + 6, 0xFFFFFF, false);
            } else {
                context.drawText(this.textRenderer, Text.literal("Invalid item"), x + 24, y + 6, 0xFF5555, false);
            }

            String cost = String.valueOf(s.cost);
            int costWidth = this.textRenderer.getWidth(cost);
            context.drawText(this.textRenderer, Text.literal(cost), x + listWidth - 8 - costWidth, y + 6, 0xFFFF55, false);
        }

        if (contentHeight > listHeight) {
            int barWidth = 6;
            int barX = listRight - barWidth - 4;
            float viewportRatio = (float) listHeight / (float) contentHeight;
            int thumbHeight = Math.max(10, (int) (listHeight * viewportRatio));
            int thumbRange = listHeight - thumbHeight;
            int thumbY = listTop + (int) ((float) scroll / (float) maxScroll * thumbRange);

            context.fill(barX, listTop, barX + barWidth, listTop + listHeight, 0x44000000);
            context.fill(barX, thumbY, barX + barWidth, thumbY + thumbHeight, 0xAA888888);
        }

        if (errorMessage != null && errorTimer > 0) {
            int msgWidth = this.textRenderer.getWidth(Text.literal(errorMessage));
            int x = (this.width - msgWidth) / 2;
            context.drawText(this.textRenderer, Text.literal(errorMessage), x, this.height - 30, 0xFF6666, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount, double delta) {
        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listTop + listHeight) {
            int deltaY = (int) Math.signum(amount);
            scroll -= deltaY * (itemHeight / 2 + 8);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount, delta);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listTop + listHeight) {
            int deltaY = (int) Math.signum(amount);
            scroll -= deltaY * (itemHeight / 2 + 8);
            clampScroll();
            return true;
        }
        return false;
    }

    private void clampScroll() {
        int contentHeight = items().size() * itemHeight;
        int maxScroll = Math.max(0, contentHeight - listHeight);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listTop + listHeight) {
            int relativeY = (int) (mouseY - listTop + scroll);
            int index = relativeY / itemHeight;
            if (index >= 0 && index < items().size()) {
                ShopItem s = items().get(index);
                ItemStack stack = s.getItem();
                if (stack.isEmpty()) {
                    setErrorMessage("Invalid item: " + s.item);
                } else {
                    setErrorMessage("Clicked " + stack.getName().getString() + " for " + s.cost + " coins");
                    // TODO: send packet to server to perform purchase
                }
                return true;
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (errorTimer > 0) {
            errorTimer--;
            if (errorTimer <= 0) {
                errorMessage = null;
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
    }
}
