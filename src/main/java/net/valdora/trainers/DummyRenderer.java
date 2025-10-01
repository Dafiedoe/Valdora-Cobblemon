package net.valdora.trainers;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.ValdoraClient;

public class DummyRenderer extends LivingEntityRenderer<DummyEntity, DummyModel> {

    private static final Identifier TEXTURE = Identifier.of(Valdora.MOD_ID, "textures/entity/dummy.png");

    public DummyRenderer(EntityRendererFactory.Context context) {
        super(context, new DummyModel(context.getPart(ValdoraClient.DUMMY_LAYER)), 0.0F);  // No shadow
    }

    @Override
    public Identifier getTexture(DummyEntity entity) {
        return TEXTURE;
    }
}