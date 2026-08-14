package net.valdora.items.repel;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class RepelItem extends Item {
    public static int DEFAULT_STEPS = 100;
    public static int SUPER_STEPS = 200;
    public static int MAX_STEPS = 250;
    
    private RepelType type;
    
    public RepelItem(Settings settings, RepelType _type) {
        super(settings);
        type = _type;
    }
    
    public int getStepsByType(RepelType type) {
        if (type == RepelType.Default) return DEFAULT_STEPS;
        else if (type == RepelType.Super) return SUPER_STEPS;
        else if (type == RepelType.Max) return MAX_STEPS;
        
        return DEFAULT_STEPS;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            if (user instanceof ServerPlayerEntity serverPlayer) {
                RepelManager.activateRepel(serverPlayer, getStepsByType(type));
            }
            
            user.sendMessage(Text.literal("A Repel was used!"), true);
            
            if (!user.getAbilities().creativeMode) {
                user.getStackInHand(hand).decrement(1);
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
