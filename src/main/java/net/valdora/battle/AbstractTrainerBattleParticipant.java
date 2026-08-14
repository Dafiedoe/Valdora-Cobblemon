package net.valdora.battle;

import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.valdora.Valdora;
import net.valdora.trainers.ConditionalConfigPokemon;
import net.valdora.trainers.ConfigPokemon;
import net.valdora.trainers.DummyEntity;
import net.valdora.trainers.TrainerConfig;
import net.valdora.utils.PokemonTeamBattleActor;

import java.util.*;

public class AbstractTrainerBattleParticipant implements TrainerBattleParticipant {
    private final String id;
    private final UUID uuid;
    private final String name;
    private final ServerPlayerEntity player;
    private final PartyStore party;
    private final BattleFormat battleFormat;
    private final BattleAI battleAI;
    private final UUID entityUuid;
    private final TrainerConfig config;
    public final String trainerNpcUuid;
    
    public AbstractTrainerBattleParticipant(TrainerConfig config, ServerPlayerEntity player, DummyEntity dummy, String trainerNpcUuid) {
        this.config = config;
        this.id = config.trainerId;
        this.uuid = UUID.randomUUID();
        this.name = config.trainerName;
        this.player = player;
        this.trainerNpcUuid = trainerNpcUuid;
        
        PartyStore trainerParty = new PartyStore(UUID.randomUUID());
        int index = 0;
        for (ConfigPokemon configPokemon : config.pokemonTeam) {
            if (configPokemon instanceof ConditionalConfigPokemon conditional) {
                if (!conditional.isAllowedForPlayer(player)) {
                    Valdora.LOGGER.info("Skipping conditional Pokémon " + conditional.species + " for trainer " + config.trainerId + " (missing flag " + conditional.requiredFlag + "=" + conditional.requiredValue + ")");
                    continue;
                }
            }
            
            Pokemon builtPkmn = configPokemon.build();
            if (builtPkmn == null) {
                Valdora.LOGGER.error("Failed to build Pokémon for trainer " + config.trainerId);
                continue;
            }
            
            builtPkmn.setOriginalTrainer(config.trainerId);
            
            if (index >= 6) {
                Valdora.LOGGER.warn("Trainer " + config.trainerId + " has more than 6 valid Pokémon. Extra Pokémon (like " + builtPkmn.getSpecies().getName() + ") will be ignored.");
                break;
            }
            
            trainerParty.set(index, builtPkmn);
            index++;
        }
        this.party = trainerParty;
        
        this.battleFormat = BattleFormat.Companion.getGEN_9_SINGLES();
        this.battleAI = new BattleAIFactory(config.battleFormat, config.aiLevel).create();
        
        this.entityUuid = dummy.getUuid();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public UUID getUuid() {
        return uuid;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public BattleFormat getBattleFormat() {
        return battleFormat;
    }
    
    @Override
    public BattleAI getBattleAI() {
        return battleAI;
    }
    
    @Override
    public PokemonTeamBattleActor createBattleActor(ServerPlayerEntity player) {
        return new PokemonTeamBattleActor(getName(), config.trainerId, trainerNpcUuid, getUuid(), getBattleTeam(player), getBattleAI(), getEntityOrPlayer(player));
    }
    
    private LivingEntity getEntityOrPlayer(ServerPlayerEntity player) {
        try {
            return getEntity(player);
        } catch (NullPointerException e) {
            return player;
        }
    }
    
    @Override
    public LivingEntity getEntity(ServerPlayerEntity player) {
        try {
            LivingEntity entity = (LivingEntity) player.getServerWorld().getEntity(entityUuid);
            
            return entity;
        } catch (ClassCastException e) {
            throw new NullPointerException();
        }
    }
    
    @Override
    public PartyStore getParty() {
        return party;
    }
    
    @Override
    public List<BattlePokemon> getBattleTeam(ServerPlayerEntity player) {
        List<BattlePokemon> team = getParty().toGappyList().stream().filter(Objects::nonNull).map((pokemon) -> {
            return new BattlePokemon(pokemon, pokemon.clone(true, player.getServer().getRegistryManager()),
                    pokemonEntity -> {
                        pokemonEntity.discard();
                        return Unit.INSTANCE;
                    }
            );
        }).toList();
        team.forEach(pokemon -> pokemon.getEffectedPokemon().heal());
        return team;
    }
    
    public TrainerConfig getConfig() {
        return config;
    }
}
