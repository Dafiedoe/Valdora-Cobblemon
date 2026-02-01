package net.valdora.quests.objectivetypes;

import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.quests.ActiveQuest;
import net.valdora.quests.Objective;
import net.valdora.quests.ObjectiveType;

public class DeliverItemObjective extends Objective {
    public Item item;

    public DeliverItemObjective(String title, String description, String questId, JsonObject json) {
        super(title, description, ObjectiveType.DELIVER_ITEM, questId);

        String itemIdStr = json.get("item_id").getAsString();
        Identifier itemIdentifier = Identifier.tryParse(itemIdStr);
        if (itemIdentifier != null) {
            item = Registries.ITEM.get(itemIdentifier);
        } else {
            Valdora.LOGGER.error("No item with id '" + itemIdStr + "' exists!");
            item = null;
        }
    }

    @Override
    public boolean handleObjectiveUpdate(ActiveQuest activeQuest, ServerPlayerEntity player, Object data) {
        if (item == null) {
            return false;
        }

        int amount = player.getInventory().count(item);
        if (amount == 0) {
            return false;
        }

        int delivered = Math.min(amount, count - activeQuest.count);
        if (delivered <= 0) {
            return false;
        }

        int toRemove = delivered;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int removeFromThis = Math.min(toRemove, stack.getCount());
                stack.decrement(removeFromThis);
                toRemove -= removeFromThis;
                if (toRemove <= 0) {
                    break;
                }
            }
        }

        activeQuest.count += delivered;

        return activeQuest.count >= count;
    }
}
