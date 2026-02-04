package net.valdora.biomespiralchanger;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicBoolean;

public class BiomeChanger {
    private static ServerPlayerEntity player = null;
    private static String biomeId = null;
    private static Thread changerThread;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final AtomicBoolean paused = new AtomicBoolean(false);
    
    // Saved origin (last setup)
    private static int x = 0;
    private static int z = 0;
    
    // Block stepping (configurable)
    private static int blockSize = 8;
    
    // Alignment choice for the starting square
    private enum Alignment { FLOOR, NEAREST, CENTER }
    private static Alignment alignment = Alignment.CENTER;
    
    /**
     * Setup the player, target biome and block size (in blocks).
     * blockSize will be rounded down to the nearest multiple of 4 and min 4.
     * Alignment defaults to CENTER. You can add an overload to expose alignment via command.
     */
    public static void Setup(ServerPlayerEntity _player, String _biomeId) {
        player = _player;
        biomeId = _biomeId;
        BlockPos pos = player.getBlockPos();
        x = pos.getX();
        z = pos.getZ();
    }
    
    /**
     * Start changing biomes in a spiral pattern. Steps are done per `blockSize` block squares.
     * If a previous run is active, it will be stopped and replaced with this new run.
     */
    public static void Start(ServerPlayerEntity commandSource) {
        if (player == null || biomeId == null) {
            commandSource.sendMessage(Text.literal("Setup has not yet been run. Use '/valdora biomechanger setup <biome> <blockSize>' first!")
                    .formatted(Formatting.RED));
            return;
        }
        
        // If something's already running, stop it first to avoid multiple threads
        if (running.get()) {
            commandSource.sendMessage(Text.literal("Stopping previous biome-changer run and starting a new one...").formatted(Formatting.YELLOW));
            Stop();
            int attempts = 0;
            while (changerThread != null && attempts++ < 10) {
                try { Thread.sleep(25); } catch (InterruptedException ignored) {}
            }
        }
        
        running.set(true);
        paused.set(false);
        
        changerThread = new Thread(() -> {
            int dx = 0;
            int dz = 1;
            int segmentLength = 1;
            int radiusCounter = 0;
            
            // Compute the starting square according to alignment choice.
            // startX..startX+blockSize-1 is the first block-square that will be filled.
            final int startX;
            final int startZ;
            switch (alignment) {
                case NEAREST:
                    startX = Math.floorDiv(x + blockSize/2, blockSize) * blockSize;
                    startZ = Math.floorDiv(z + blockSize/2, blockSize) * blockSize;
                    break;
                case FLOOR:
                    startX = Math.floorDiv(x, blockSize) * blockSize;
                    startZ = Math.floorDiv(z, blockSize) * blockSize;
                    break;
                case CENTER:
                default:
                    // Center the block-square around the player as best as integer arithmetic allows.
                    // We compute a candidate start so the player is near the center of the block-square,
                    // then do not force it to be blockSize-multiple — fillBiomeBlockAt will expand to 4x4 biome cells properly.
                    int halfIndex = (blockSize - 1) / 2; // integer
                    startX = x - halfIndex;
                    startZ = z - halfIndex;
                    break;
            }
            
            int currentX = startX;
            int currentZ = startZ;
            
            // Immediately apply the first block and print debug info
            fillBiomeBlockAt(currentX, currentZ, /*debugFirst=*/true);
            
            while (running.get()) {
                while (paused.get()) {
                    try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
                }
                
                // Step by blockSize in spiral pattern
                currentX += dx * blockSize;
                currentZ += dz * blockSize;
                
                fillBiomeBlockAt(currentX, currentZ, /*debugFirst=*/false);
                
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
    
    public static void Pause() {
        paused.set(true);
    }
    
    public static void Resume() {
        paused.set(false);
    }
    
    /**
     * Fill the biome for a block-aligned square starting at (startX, startZ) of size blockSize.
     * This method computes the 4x4-aligned biome cell extents and requests the server to run the
     * /fillbiome command on the main server thread.
     *
     * debugFirst prints extra info about how the start was computed for the very first block.
     */
    private static void fillBiomeBlockAt(int startX, int startZ, boolean debugFirst) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        World world = player.getWorld();
        
        // Inclusive end coords for the block region
        int endX = startX + blockSize - 1;
        int endZ = startZ + blockSize - 1;
        
        // Expand to 4x4 biome cell boundaries (inclusive)
        int cellStartX4 = Math.floorDiv(Math.min(startX, endX), 4) * 4;
        int cellStartZ4 = Math.floorDiv(Math.min(startZ, endZ), 4) * 4;
        int cellEndX4 = Math.floorDiv(Math.max(startX, endX), 4) * 4 + 3;
        int cellEndZ4 = Math.floorDiv(Math.max(startZ, endZ), 4) * 4 + 3;
        
        // Quick chunk-loaded check at center of the cell range
        int centerX = (cellStartX4 + cellEndX4) / 2;
        int centerZ = (cellStartZ4 + cellEndZ4) / 2;
        BlockPos checkPos = new BlockPos(centerX, 0, centerZ);
        if (!world.isChunkLoaded(checkPos)) return;
        
        int yStart = world.getBottomY();
        int yEnd = world.getTopY() - 1;
        
        final String command = String.format("/fillbiome %d %d %d %d %d %d %s",
                cellStartX4, yStart, cellStartZ4,
                cellEndX4,   yEnd,   cellEndZ4,
                biomeId
        );
        
        server.execute(() -> {
            server.getCommandManager().executeWithPrefix(player.getCommandSource(), command);
            
            // DEBUG: show player's block, alignment, computed start and resulting ranges
            if (debugFirst) {
                BlockPos playerPos = player.getBlockPos();
                int playerBlockX = playerPos.getX();
                int playerBlockZ = playerPos.getZ();
                
                String debug = String.format("playerBlock=(%d,%d) alignment=%s -> blockStart=(%d,%d) blockRangeX=[%d..%d] blockRangeZ=[%d..%d] cellRangeX=[%d..%d] cellRangeZ=[%d..%d]",
                        playerBlockX, playerBlockZ,
                        alignment,
                        startX, startZ,
                        startX, endX,
                        startZ, endZ,
                        cellStartX4, cellEndX4,
                        cellStartZ4, cellEndZ4
                );
                
                player.sendMessage(Text.literal(debug).formatted(Formatting.GRAY), false);
            }
        });
    }
}
