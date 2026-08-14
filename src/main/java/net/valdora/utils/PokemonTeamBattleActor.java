package net.valdora.utils;

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

public class PokemonTeamBattleActor extends AIBattleActor implements EntityBackedBattleActor<LivingEntity>, FleeableBattleActor {
    private final String name;
    private final String id;
    private final String npcUuid;
    private final LivingEntity entity;
    private final ServerWorld world;
    private final Vec3d pos;
    
    public PokemonTeamBattleActor(String name, String id, String npcUuid, @NotNull UUID uuid, @NotNull List<BattlePokemon> pokemonList, @NotNull BattleAI battleAI, LivingEntity entity) {
        super(uuid, pokemonList, battleAI);
        this.name = name;
        this.id = id;
        this.npcUuid = npcUuid;
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
    public MutableText nameOwned(@NotNull String pokemonName) {
        return Text.literal(this.name + "'s " + pokemonName);
    }
    
    public String getId() {
        return id;
    }
    
    public String getNpcUuid() {
        return npcUuid;
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
