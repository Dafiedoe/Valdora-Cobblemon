package net.valdora.savedata.flaggedbarrier;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FlaggedBarrierEntityRenderer implements BlockEntityRenderer<FlaggedBarrierEntity> {
    public FlaggedBarrierEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }
    
    @Override
    public void render(FlaggedBarrierEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        
        ItemStack mainHand = player.getMainHandStack();
        if (!(mainHand.getItem() instanceof FlaggedBarrierItem)) {
            return;
        }
        
        BlockState state = entity.getCachedState();
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(state, entity.getPos(), entity.getWorld(), matrices, vertexConsumers.getBuffer(RenderLayer.getTranslucent()), false,
                entity.getWorld().getRandom());
    }
}