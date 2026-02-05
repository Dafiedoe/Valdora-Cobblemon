package net.valdora.biomespiralchanger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;

import net.valdora.Valdora;
import net.valdora.utils.TickScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BiomeChanger working in chunk coordinates with robust splitting to respect the /fillbiome volume limit.
 *
 * Fix: start the spiral facing east (dx=1,dz=0) so the west neighbor isn't skipped.
 */
public class BiomeChanger {
    private static ServerPlayerEntity player = null;
    private static String biomeId = null;
    private static Thread changerThread;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final AtomicBoolean paused = new AtomicBoolean(false);
    
    private static int x = 0;
    private static int z = 0;
    
    // player's chunk at Setup time — we will always start the spiral here
    private static int originChunkX = 0;
    private static int originChunkZ = 0;
    
    private static int chunkSize = 1;
    
    private enum Alignment { FLOOR, NEAREST, CENTER }
    private static Alignment alignment = Alignment.CENTER;
    
    private static final long MAX_VOLUME = 32768L;
    
    private static class Region {
        final int sx, ex, sz, ez;
        Region(int sx, int ex, int sz, int ez) {
            this.sx = sx; this.ex = ex; this.sz = sz; this.ez = ez;
        }
    }
    
    public static void Setup(ServerPlayerEntity _player, String _biomeId) {
        player = _player;
        biomeId = _biomeId;
        
        BlockPos pos = player.getBlockPos();
        x = pos.getX();
        z = pos.getZ();
        
        originChunkX = Math.floorDiv(x, 16);
        originChunkZ = Math.floorDiv(z, 16);
    }
    
    public static void SetChunkSize(int size) {
        chunkSize = Math.max(1, size);
    }
    
    public static void Start(ServerPlayerEntity commandSource) {
        if (player == null || biomeId == null) {
            commandSource.sendMessage(
                    Text.literal("Setup has not yet been run. Use '/valdora biomechanger setup <biome> <chunkSize>' first!")
                            .formatted(Formatting.RED)
            );
            return;
        }
        
        if (running.get()) {
            commandSource.sendMessage(
                    Text.literal("Stopping previous biome-changer run and starting a new one...")
                            .formatted(Formatting.YELLOW)
            );
            Stop();
            int attempts = 0;
            while (changerThread != null && attempts++ < 10) {
                try { Thread.sleep(25); } catch (InterruptedException ignored) {}
            }
        }
        
        running.set(true);
        paused.set(false);
        
        changerThread = new Thread(() -> {
            // **Start facing EAST** so spiral visits west neighbor properly.
            int dx = 1;
            int dz = 0;
            int segmentLength = 1;
            int radiusCounter = 0;
            
            // Use the stored origin chunk from Setup — always start the spiral at the player's chunk.
            final int playerChunkX = originChunkX;
            final int playerChunkZ = originChunkZ;
            
            // Start at the player's chunk to ensure center is processed.
            final int startChunkX = playerChunkX;
            final int startChunkZ = playerChunkZ;
            
            int currentChunkX = startChunkX;
            int currentChunkZ = startChunkZ;
            
            fillBiomeChunkAt(currentChunkX, currentChunkZ, /*debugFirst=*/true);
            
            while (running.get()) {
                while (paused.get()) {
                    try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
                }
                
                currentChunkX += dx;
                currentChunkZ += dz;
                
                fillBiomeChunkAt(currentChunkX, currentChunkZ, /*debugFirst=*/false);
                
                radiusCounter++;
                if (radiusCounter == segmentLength) {
                    radiusCounter = 0;
                    int temp = dx;
                    dx = -dz;
                    dz = temp;
                    if (dz == 0) segmentLength++;
                }
                
                try { Thread.sleep(30); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }, "Valdora-BiomeChanger");
        
        changerThread.setDaemon(true);
        changerThread.start();
    }
    
    public static void Stop() {
        running.set(false);
        if (changerThread != null) {
            changerThread.interrupt();
            changerThread = null;
        }
    }
    
    public static void Pause() { paused.set(true); }
    public static void Resume() { paused.set(false); }
    
    private static void fillBiomeChunkAt(int chunkX, int chunkZ, boolean debugFirst) {
        final int blockStartX = chunkX * 16;
        final int blockStartZ = chunkZ * 16;
        final int blockEndX = (chunkX + chunkSize) * 16 - 1;
        final int blockEndZ = (chunkZ + chunkSize) * 16 - 1;
        
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        server.execute(() -> {
            ServerWorld serverWorld = (ServerWorld) player.getWorld();
            
            final int minChunkX = chunkX;
            final int maxChunkX = chunkX + chunkSize - 1;
            final int minChunkZ = chunkZ;
            final int maxChunkZ = chunkZ + chunkSize - 1;
            
            // Force and synchronously request chunks
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    try {
                        serverWorld.setChunkForced(cx, cz, true);
                        serverWorld.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, true);
                    } catch (Exception e) {
                        Valdora.LOGGER.warn("Failed to force/load chunk {} {}, exception: {}", cx, cz, e.toString());
                    }
                }
            }
            
            int yStart = serverWorld.getBottomY();
            int yEnd = serverWorld.getTopY() - 1;
            final int ySize = yEnd - yStart + 1;
            
            // initial 4-way partition
            List<Region> parts = splitIntoFour(blockStartX, blockEndX, blockStartZ, blockEndZ);
            
            if (debugFirst) {
                BlockPos playerPos = player.getBlockPos();
                String debug = String.format("playerBlock=(%d,%d) chunkStart=(%d,%d) chunkSize=%d -> blockRangeX=[%d..%d] blockRangeZ=[%d..%d] initialParts=%d",
                        playerPos.getX(), playerPos.getZ(),
                        chunkX, chunkZ,
                        chunkSize,
                        blockStartX, blockEndX,
                        blockStartZ, blockEndZ,
                        parts.size());
                player.sendMessage(Text.literal(debug).formatted(Formatting.GRAY), false);
            }
            
            // Build a list of concrete fill commands (handles horizontal recursion and vertical slicing)
            List<String> fillCommands = new ArrayList<>();
            
            // Worklist for horizontal splitting when footprint alone exceeds MAX_VOLUME
            List<Region> work = new ArrayList<>(parts);
            while (!work.isEmpty()) {
                Region r = work.remove(0);
                
                // skip degenerate regions
                if (r.sx > r.ex || r.sz > r.ez) continue;
                
                long xLen = (long) r.ex - r.sx + 1L;
                long zLen = (long) r.ez - r.sz + 1L;
                
                // if even a 1-high slice is too big horizontally, split horizontally
                if (xLen * zLen > MAX_VOLUME) {
                    // split along longer horizontal axis
                    if (xLen >= zLen) {
                        int midX = Math.floorDiv(r.sx + r.ex, 2);
                        if (midX < r.sx || midX >= r.ex) {
                            // fallback: treat as-is (shouldn't normally happen)
                            work.add(r);
                        } else {
                            work.add(0, new Region(midX + 1, r.ex, r.sz, r.ez));
                            work.add(0, new Region(r.sx, midX, r.sz, r.ez));
                        }
                    } else {
                        int midZ = Math.floorDiv(r.sz + r.ez, 2);
                        if (midZ < r.sz || midZ >= r.ez) {
                            work.add(r);
                        } else {
                            work.add(0, new Region(r.sx, r.ex, midZ + 1, r.ez));
                            work.add(0, new Region(r.sx, r.ex, r.sz, midZ));
                        }
                    }
                    continue;
                }
                
                // horizontal footprint is small enough; compute vertical slices
                long footprint = xLen * zLen;
                // sliceHeight such that footprint * sliceHeight <= MAX_VOLUME
                int sliceHeight = (int) Math.max(1L, MAX_VOLUME / footprint);
                // how many full slices needed to cover ySize
                int slices = (int) ((ySize + sliceHeight - 1) / sliceHeight);
                
                for (int s = 0; s < slices; s++) {
                    int sliceYStart = yStart + s * sliceHeight;
                    int sliceYEnd = Math.min(yEnd, sliceYStart + sliceHeight - 1);
                    
                    int sx4 = Math.floorDiv(r.sx, 4) * 4;
                    int sz4 = Math.floorDiv(r.sz, 4) * 4;
                    int ex4 = Math.floorDiv(r.ex, 4) * 4 + 3;
                    int ez4 = Math.floorDiv(r.ez, 4) * 4 + 3;
                    
                    String cmd = String.format("/fillbiome %d %d %d %d %d %d %s",
                            sx4, sliceYStart, sz4,
                            ex4, sliceYEnd, ez4,
                            biomeId);
                    fillCommands.add(cmd);
                }
            }
            
            // schedule fill commands staggered by 1 tick each (baseDelay gives a tick for chunk load to settle)
            int baseDelay = 1;
            for (int i = 0; i < fillCommands.size(); i++) {
                final String cmd = fillCommands.get(i);
                final int delay = baseDelay + i;
                TickScheduler.runNextTick(delay, () -> {
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd);
                });
            }
            
            // unforce after all done with a safety delay
            int unforceDelay = baseDelay + fillCommands.size() + 2;
            TickScheduler.runNextTick(unforceDelay, () -> {
                for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                    for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                        serverWorld.setChunkForced(cx, cz, false);
                    }
                }
            });
        });
    }
    
    /**
     * split rectangle into four quadrants (may include degenerate quadrants)
     * Use Math.floorDiv for midpoint so negatives behave correctly.
     */
    private static List<Region> splitIntoFour(int sx, int ex, int sz, int ez) {
        List<Region> out = new ArrayList<>(4);
        int midX = Math.floorDiv(sx + ex, 2);
        int midZ = Math.floorDiv(sz + ez, 2);
        
        out.add(new Region(sx,      midX, sz,      midZ));      // NW
        out.add(new Region(midX+1,  ex,   sz,      midZ));      // NE
        out.add(new Region(sx,      midX, midZ+1,  ez));       // SW
        out.add(new Region(midX+1,  ex,   midZ+1,  ez));       // SE
        
        return out;
    }
}
