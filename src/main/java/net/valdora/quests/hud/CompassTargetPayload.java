package net.valdora.quests.hud;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public record CompassTargetPayload(double x, double y, double z, boolean showCompass) implements CustomPayload {
    public static final Id<CompassTargetPayload> ID = new Id<>(Identifier.of(Valdora.MOD_ID, "compass_target"));
    public static final PacketCodec<RegistryByteBuf, CompassTargetPayload> CODEC = PacketCodec.tuple(PacketCodecs.DOUBLE, CompassTargetPayload::x, PacketCodecs.DOUBLE, CompassTargetPayload::y,
            PacketCodecs.DOUBLE, CompassTargetPayload::z, PacketCodecs.BOOL, CompassTargetPayload::showCompass, CompassTargetPayload::new);
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
