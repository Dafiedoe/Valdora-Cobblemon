package net.valdora.general;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SurfBoardEntity extends BoatEntity {
    public SurfBoardEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public SurfBoardEntity(World world, double x, double y, double z) {
        this(ModEntities.SURF_BOARD, world);
        this.setPosition(x, y, z);
        this.setVelocity(Vec3d.ZERO);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
    }
    
    @Override
    public void tick() {
        super.tick();
        if (this.hasPassengers()) {
            Entity passenger = this.getFirstPassenger();
            if (passenger instanceof PlayerEntity player) {
                if (player.isSneaking()) {
                    player.stopRiding();
                }
            }
        }
    }
}
