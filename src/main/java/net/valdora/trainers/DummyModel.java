package net.valdora.trainers;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class DummyModel extends EntityModel<DummyEntity> {

    public DummyModel(ModelPart root) {
        // No parts needed
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        // Empty model (no cuboids or parts)
        return TexturedModelData.of(modelData, 1, 1);  // Minimal texture size
    }

    @Override
    public void setAngles(DummyEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // No angles or animations
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        // Render nothing
    }
}