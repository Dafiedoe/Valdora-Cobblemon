package net.valdora.battle;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.FleeableBattleActor;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import kotlin.Pair;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EntityBackedTrainerBattleActor extends AIBattleActor implements EntityBackedBattleActor<LivingEntity>, FleeableBattleActor {
    private final String name;
    private final LivingEntity entity;
    private final ServerWorld world;
    private final Vec3d pos;

    public EntityBackedTrainerBattleActor(
            String name,
            UUID uuid,
            List<BattlePokemon> pokemon,
            BattleAI battleAI,
            LivingEntity entity
    ) {
        super(uuid, pokemon, battleAI);
        this.name = name;
        this.entity = entity;
        this.world = (ServerWorld) entity.getWorld();
        this.pos = entity.getPos();
    }

    @Override
    public LivingEntity getEntity() {
        return this.entity;
    }

    @NotNull
    @Override
    public ActorType getType() {
        return ActorType.NPC;
    }

    @NotNull
    @Override
    public MutableText getName() {
        return Text.literal(this.name);
    }

    @NotNull
    @Override
    public MutableText nameOwned(@NotNull String s) {
        return Text.literal(s).append(getName());
    }

    @Nullable
    @Override
    public Pair<ServerWorld, Vec3d> getWorldAndPosition() {
        return new Pair<>(world, pos);
    }

    @Nullable
    @Override
    public Vec3d getInitialPos() {
        return entity.getPos();
    }

    @Override
    public float getFleeDistance() {
        return 0;
    }
}