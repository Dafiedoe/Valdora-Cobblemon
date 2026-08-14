package net.valdora.savedata.flaggedbarrier;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.valdora.general.ModComponents;
import net.valdora.savedata.PlayerSaveDataManager;

import java.util.Map;

public class FlaggedBarrierBlock extends BlockWithEntity {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FlaggedBarrierBlock.class);
    private static final boolean DEBUG = false;
    
    public FlaggedBarrierBlock(Settings settings) {
        super(settings
                .noCollision()
                .nonOpaque()
                .luminance(state -> 0)
                .noBlockBreakParticles()
                .strength(-1.0F, 3600000.0F)
                .suffocates(((state, world, pos) -> false))
        );
    }
    
    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }
    
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlaggedBarrierEntity(pos, state);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }
    
    @Override
    public int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return 0;
    }
    
    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }
    
    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0F;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView worldView, BlockPos pos, ShapeContext context) {
        PlayerEntity player = null;
        
        if (context instanceof EntityShapeContext entityContext) {
            if (entityContext.getEntity() instanceof PlayerEntity p) {
                player = p;
            }
        }
        
        if (player == null && worldView instanceof World world && !world.isClient) {
            final double maxDist = 2.5D;
            player = world.getClosestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, maxDist, p -> true);
            
            if (player != null) {
                double dx = player.getX() - (pos.getX() + 0.5D);
                double dy = player.getY() - (pos.getY() + 0.5D);
                double dz = player.getZ() - (pos.getZ() + 0.5D);
                double dist2 = dx * dx + dy * dy + dz * dz;
                if (dist2 > (maxDist * maxDist)) {
                    player = null;
                }
            }
        }
        
        if (player == null) {
            return VoxelShapes.fullCube();
        }
        
        BlockEntity beRaw = worldView.getBlockEntity(pos);
        if (!(beRaw instanceof FlaggedBarrierEntity be)) {
            return VoxelShapes.fullCube();
        }
        
        String flagName = be.getFlagName();
        String flagValue = be.getFlagValue();
        if (flagName == null) flagName = "";
        if (flagValue == null) flagValue = "";
        
        if (flagName.isEmpty() || flagValue.isEmpty()) {
            return VoxelShapes.fullCube();
        }
        
        try {
            if (worldView instanceof World w && w.isClient) {
                Map<String, String> flags = ClientPlayerFlagCache.getFlags(player.getUuid());
                if (flags != null && flagValue.equals(flags.get(flagName))) {
                    return VoxelShapes.empty();
                } else {
                    return VoxelShapes.fullCube();
                }
            } else {
                boolean passes = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid()).checkFlag(flagName, flagValue);
                if (passes) {
                    return VoxelShapes.empty();
                }
            }
        } catch (Throwable t) {
            if (DEBUG) {
                LOGGER.warn("[FlaggedBarrier] exception while checking flags for player {}: {}", player == null ? "null" : player.getUuid(), t.toString());
            }
        }
        
        return VoxelShapes.fullCube();
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext entityContext && entityContext.getEntity() instanceof PlayerEntity player) {
            ItemStack mainHand = player.getMainHandStack();
            if (mainHand.getItem() instanceof FlaggedBarrierItem) {
                return VoxelShapes.fullCube();
            }
        }
        return VoxelShapes.empty();
    }
    
    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof FlaggedBarrierItem) {
            float hardness = 1.5F;
            return player.getBlockBreakingSpeed(state) / hardness / 30.0F;
        }
        return 0.0F;
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        if (world instanceof ServerWorld) {
            if (world.getBlockEntity(pos) instanceof FlaggedBarrierEntity be) {
                String name = itemStack.getOrDefault(ModComponents.FLAG_NAME, "");
                String value = itemStack.getOrDefault(ModComponents.FLAG_VALUE, "");
                if (!name.isEmpty() || !value.isEmpty()) {
                    be.setFlagData(name, value);
                }
            }
        }
    }
}
