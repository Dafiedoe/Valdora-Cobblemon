package net.valdora.general;

import net.minecraft.block.BlockState;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SurfManager {
    private static final Map<ServerPlayerEntity, SurfBoardEntity> surfingPlayers = new ConcurrentHashMap<>();
    private static final int LAND_SEARCH_RADIUS = 64;

    public static void tick(ServerPlayerEntity player) {
        World world = player.getWorld();

        boolean inWater = player.isTouchingWater();
        boolean hasSurfHM = PokemonPartyApi.hasSurf(player);

        // prevent entering water without Surf HM
        if (inWater && !hasSurfHM) {
            // notify player
            player.sendMessage(Text.literal("You need a Pokémon with Surf to enter the water!"), false);
            // teleport to nearest land block
            BlockPos landPos = findNearestLandPos(world, player.getBlockPos());
            if (landPos != null) {
                player.teleport(landPos.getX() + 0.5, landPos.getY() + 0.1, landPos.getZ() + 0.5, false);
            }
            return;
        }

        // don’t touch real boats
        if (player.getVehicle() instanceof BoatEntity) return;

        // cleanup if player dismounted manually
        SurfBoardEntity currentBoard = surfingPlayers.get(player);
        if (currentBoard != null && player.getVehicle() != currentBoard) {
            teleportToLandIfAdjacent(player, currentBoard.getBlockPos());
            surfingPlayers.remove(player);
            currentBoard.discard();
        }

        // spawn surf board when Surf HM and at water surface
        if (inWater && hasSurfHM && !surfingPlayers.containsKey(player)) {
            BlockPos waterPos = findWaterSurfacePos(world, player.getBlockPos());
            if (waterPos != null && !world.getFluidState(waterPos.up()).isIn(FluidTags.WATER)) {
                var state = world.getFluidState(waterPos);
                double spawnY = waterPos.getY() + state.getHeight(world, waterPos);

                SurfBoardEntity board = new SurfBoardEntity(ModEntities.SURF_BOARD, world);
                board.refreshPositionAndAngles(
                        player.getX(),
                        spawnY,
                        player.getZ(),
                        player.getYaw(),
                        player.getPitch()
                );

                world.spawnEntity(board);
                player.startRiding(board, true);
                surfingPlayers.put(player, board);
            }
        }

        // remove surf board when leaving water or losing Surf HM
        if ((!inWater || !hasSurfHM) && surfingPlayers.containsKey(player)) {
            SurfBoardEntity board = surfingPlayers.remove(player);
            if (board != null && board.hasPassenger(player)) {
                player.stopRiding();
                teleportToLandIfAdjacent(player, board.getBlockPos());
                board.discard();
            }
        }
    }

    private static boolean isPlayerInWater(ServerPlayerEntity player) {
        return player.isTouchingWater();
    }

    private static BlockPos findWaterSurfacePos(World world, BlockPos origin) {
        for (int i = 0; i <= 5; i++) {
            BlockPos pos = origin.down(i);
            if (world.getFluidState(pos).isIn(FluidTags.WATER)) {
                return pos;
            }
        }
        return null;
    }

    private static void teleportToLandIfAdjacent(ServerPlayerEntity player, BlockPos waterPos) {
        World world = player.getWorld();
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = waterPos.offset(dir);
            BlockState state = world.getBlockState(neighbor);
            if (!world.getFluidState(neighbor).isIn(FluidTags.WATER) && !state.isAir()) {
                BlockPos landSurface = neighbor.up();
                player.teleport(
                        landSurface.getX() + 0.5,
                        landSurface.getY() + 0.1,
                        landSurface.getZ() + 0.5,
                        false);
                return;
            }
        }
    }

    private static BlockPos findNearestLandPos(World world, BlockPos origin) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!world.getFluidState(pos).isIn(FluidTags.WATER) && !world.getBlockState(pos).isAir()) {
                BlockPos above = pos.up();
                BlockPos head = above.up();
                if (!world.getFluidState(above).isIn(FluidTags.WATER) && world.getBlockState(above).isAir() &&
                        !world.getFluidState(head).isIn(FluidTags.WATER) && world.getBlockState(head).isAir()) {
                    return above;
                }
            }
            if (pos.getManhattanDistance(origin) >= LAND_SEARCH_RADIUS) continue;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.offset(dir);
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return null;
    }
}