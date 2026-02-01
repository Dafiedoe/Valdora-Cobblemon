package net.valdora.shops;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

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

    private boolean showPurchaseOverlay = false;
    private ShopItem selectedItem = null;
    private int purchaseAmount = 1;

    private int playerPokedollars = 0;

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

        this.clearChildren();

        listLeft = padding;
        listTop = 40;
        listRight = this.width - padding;
        listWidth = listRight - listLeft;
        listHeight = this.height - listTop - padding;

        if (showPurchaseOverlay && selectedItem != null) {
            int panelWidth = 180;
            int panelHeight = 110;
            int x = (this.width - panelWidth) / 2;
            int y = (this.height - panelHeight) / 2;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> adjustAmount(-1))
                    .dimensions(x + 16, y + 36, 20, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> adjustAmount(1))
                    .dimensions(x + panelWidth - 36, y + 36, 20, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> closePurchaseOverlay())
                    .dimensions(x + 16, y + panelHeight - 30, 60, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Buy"), b -> confirmPurchase())
                    .dimensions(x + panelWidth - 76, y + panelHeight - 30, 60, 20).build());
        }
        else {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), (b) -> this.client.player.closeHandledScreen())
                    .dimensions(this.width - 80, this.height - 30, 70, 20).build());
        }
    }

    private List<ShopItem> items() {
        return shopConfig.items == null ? List.of() : shopConfig.items;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int titleX = this.width / 2 - this.textRenderer.getWidth(this.title) / 2;
        context.drawText(this.textRenderer, this.title, titleX, 10, 0xFFFFFF, false);

        String balance = "₽ " + playerPokedollars;
        int balanceWidth = this.textRenderer.getWidth(balance);
        context.drawText(this.textRenderer, balance,
                this.width - balanceWidth - 10, 10, 0x55FF55, false);

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

            ItemStack stack = s.getItem(1);
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
            int thumbY = listTop + (maxScroll == 0 ? 0 : (int) ((float) scroll / (float) maxScroll * thumbRange));

            context.fill(barX, listTop, barX + barWidth, listTop + listHeight, 0x44000000);
            context.fill(barX, thumbY, barX + barWidth, thumbY + thumbHeight, 0xAA888888);
        }

        if (showPurchaseOverlay && selectedItem != null) {
            context.fill(0, 0, this.width, this.height, 0x22000000);

            int panelWidth = 180;
            int panelHeight = 110;
            int x = (this.width - panelWidth) / 2;
            int y = (this.height - panelHeight) / 2;

            context.fill(x, y, x + panelWidth, y + panelHeight, 0xCCEEEEEE);

            context.drawBorder(x, y, panelWidth, panelHeight, 0x88000000);

            ItemStack stack = selectedItem.getItem(1);
            if (!stack.isEmpty()) {
                context.drawItem(stack, x + 8, y + 8);
                context.drawText(this.textRenderer, stack.getName(), x + 30, y + 12, 0x000000, false);
            } else {
                context.drawText(this.textRenderer, Text.literal("Invalid item"), x + 30, y + 12, 0xFF5555, false);
            }

            String amt = "Amount: " + purchaseAmount;
            context.drawText(this.textRenderer, Text.literal(amt),
                    x + panelWidth / 2 - this.textRenderer.getWidth(amt) / 2, y + 42, 0x000000, false);

            String total = "Total: " + (selectedItem.cost * purchaseAmount);
            context.drawText(this.textRenderer, Text.literal(total),
                    x + panelWidth / 2 - this.textRenderer.getWidth(total) / 2, y + 60, 0xAA0000, false);
        }

        super.render(context, mouseX, mouseY, delta);

        if (errorMessage != null && errorTimer > 0) {
            int msgWidth = this.textRenderer.getWidth(Text.literal(errorMessage));
            int x = (this.width - msgWidth) / 2;
            context.drawText(this.textRenderer, Text.literal(errorMessage), x, this.height - 30, 0xFF6666, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount, double delta) {
        if (showPurchaseOverlay) return true;

        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listTop + listHeight) {
            int deltaY = (int) Math.signum(amount);
            scroll -= deltaY * (itemHeight / 2 + 8);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount, delta);
    }

    private void clampScroll() {
        int contentHeight = items().size() * itemHeight;
        int maxScroll = Math.max(0, contentHeight - listHeight);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showPurchaseOverlay && selectedItem != null) {
            int panelWidth = 180;
            int panelHeight = 110;
            int x = (this.width - panelWidth) / 2;
            int y = (this.height - panelHeight) / 2;

            if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + panelHeight) {
                closePurchaseOverlay();
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listTop + listHeight) {
            int relativeY = (int) (mouseY - listTop + scroll);
            int index = relativeY / itemHeight;
            if (index >= 0 && index < items().size()) {
                ShopItem s = items().get(index);
                ItemStack stack = s.getItem(1);
                if (stack.isEmpty()) {
                    setErrorMessage("Invalid item: " + s.item);
                } else {
                    selectedItem = s;
                    purchaseAmount = 1;
                    openPurchaseOverlay();
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
        if (showPurchaseOverlay && keyCode == 256) { // ESC key code
            closePurchaseOverlay();
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

    public void setPlayerPokedollars(int amount) {
        this.playerPokedollars = amount;
    }

    private void openPurchaseOverlay() {
        showPurchaseOverlay = true;
        purchaseAmount = 1;
        this.init();
    }

    private void closePurchaseOverlay() {
        showPurchaseOverlay = false;
        selectedItem = null;
        purchaseAmount = 1;
        this.init();
    }

    private void adjustAmount(int step) {
        int change = hasShiftDown() ? step * 8 : step;
        if (purchaseAmount == 1 && change == 8) change = 7;
        purchaseAmount = Math.max(1, Math.min(64, purchaseAmount + change));
    }

    private void confirmPurchase() {
        if (selectedItem == null) return;

        PurchaseC2SPayload payload = new PurchaseC2SPayload(shopConfig.id, selectedItem.item, purchaseAmount);
        ClientPlayNetworking.send(payload);

        setPlayerPokedollars(playerPokedollars - (selectedItem.cost * purchaseAmount));

        closePurchaseOverlay();
    }
}
