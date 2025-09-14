package net.valdora.mixin;

import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.ranges.IntRange;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DropTable.class)
public class DropTableMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    public void cancelDrop(LivingEntity entity, ServerWorld world, Vec3d pos, ServerPlayerEntity player, IntRange amount, Pokemon pokemon, CallbackInfo ci) {
        ci.cancel();
    }
}