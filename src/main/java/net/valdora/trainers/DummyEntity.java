package net.valdora.trainers;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.world.World;

import java.util.Collections;

public class DummyEntity extends LivingEntity {

    public DummyEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
        this.setInvisible(true);  // Makes it invisible
        this.setNoGravity(true);  // No falling or gravity
        this.setInvulnerable(true);  // Can't be damaged or killed
    }

    // Required abstract methods from LivingEntity
    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        // Do nothing
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;  // Arbitrary, since no arms
    }

    // Minimal attributes (based on base living entity defaults)
    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    // Optional: Override tick to ensure it does absolutely nothing extra
    @Override
    public void tick() {
        // Minimal tick; no AI, movement, or updates
        super.baseTick();  // Only base entity updates
    }
}