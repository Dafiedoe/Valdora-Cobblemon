package net.valdora.npc.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.valdora.npc.custom.BaseNPC;

public class StaticNPC extends PathAwareEntity implements BaseNPC {
    public StaticNPC(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setAiDisabled(true);
        this.setInvulnerable(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void initNPC() {
    }

    @Override
    public String getTypeName() {
        return "static_npc";
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }
}