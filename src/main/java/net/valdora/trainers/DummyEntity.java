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
        this.setInvisible(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }
    
    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.emptyList();
    }
    
    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) { }
    
    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }
    
    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }
    
    @Override
    public void tick() {
        super.baseTick();
    }
}
