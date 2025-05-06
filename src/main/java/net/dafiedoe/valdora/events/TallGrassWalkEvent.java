package net.dafiedoe.valdora.events;

import net.dafiedoe.valdora.spawning.BiomeSpawnSettings;
import net.dafiedoe.valdora.spawning.SpawnPoolManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static net.dafiedoe.valdora.utils.PokemonUtils.isPlayerInBattle;
import static net.dafiedoe.valdora.utils.PokemonUtils.startWildBattle;

public class TallGrassWalkEvent {
    private static final Map<ServerPlayerEntity, Integer> ticksInSpawnRegion = new HashMap<>();
    private static final Random random = new Random();

    public static int MIN_TICKS_BEFORE_ENCOUNTER = 20;
    public static double ENCOUNTER_CHANCE_PER_TICK = 0.05;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Vec3d pos = player.getPos();
                BlockPos blockPos = BlockPos.ofFloored(pos);
                World world = player.getWorld();

                String biomeId = world.getBiome(blockPos).getKey().get().getValue().getPath();
                BiomeSpawnSettings settings = SpawnPoolManager.getSettingsForBiome(biomeId);

                if (settings == null) {
                    continue;
                }

                if (settings.spawn_block_required) {
                    Block currentBlock = world.getBlockState(blockPos).getBlock();
                    Block requiredBlock = Registries.BLOCK.get(Identifier.of(settings.spawn_block));

                    if (!currentBlock.equals(requiredBlock)) {
                        ticksInSpawnRegion.remove(player);
                        continue;
                    }
                }

                if (isPlayerInBattle(player)) {
                    continue;
                }

                int ticks = ticksInSpawnRegion.getOrDefault(player, 0) + 1;
                ticksInSpawnRegion.put(player, ticks);

                if (ticks >= MIN_TICKS_BEFORE_ENCOUNTER) {
                    if (random.nextDouble() < ENCOUNTER_CHANCE_PER_TICK) {
                        startWildBattle(player);
                        ticksInSpawnRegion.remove(player);
                    }
                }

                /*
                boolean isInGrass = world.getBlockState(blockPos).getBlock() == Blocks.TALL_GRASS;

                if (!isInGrass) {
                    ticksInSpawnRegion.remove(player);
                    continue;
                }

                if (isPlayerInBattle(player)) {
                    continue;
                }

                int ticks = ticksInSpawnRegion.getOrDefault(player, 0) + 1;
                ticksInSpawnRegion.put(player, ticks);

                if (ticks >= MIN_TICKS_BEFORE_ENCOUNTER) {
                    if (random.nextDouble() < ENCOUNTER_CHANCE_PER_TICK) {
                        startWildBattle(player);
                        ticksInSpawnRegion.remove(player);
                    }
                }
                 */
            }
        });
    }
}
