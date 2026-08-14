package net.valdora.areanotifications;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record AreaNotificationPayload(String message) implements CustomPayload {
    public static final Id<AreaNotificationPayload> ID = new Id<>(AreaNotificationManager.AREA_NOTIF_CHANNEL);
    public static final PacketCodec<RegistryByteBuf, AreaNotificationPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, AreaNotificationPayload::message, AreaNotificationPayload::new);
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}