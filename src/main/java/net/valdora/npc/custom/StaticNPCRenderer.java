package net.valdora.npc.custom;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public class StaticNPCRenderer extends MobEntityRenderer<StaticNPC, PlayerEntityModel<StaticNPC>> {

    private static final Identifier TEXTURE = Identifier.of(Valdora.MOD_ID, "textures/entity/static_npc.png");

    public StaticNPCRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(StaticNPC entity) {
        return TEXTURE;
    }
}
