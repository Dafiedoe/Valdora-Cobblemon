package net.valdora.shops;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public record PurchaseC2SPayload(String shopId, String itemId, int amount) implements CustomPayload {
    public static final Id<PurchaseC2SPayload> ID = new Id<>(Identifier.of(Valdora.MOD_ID, "purchase"));
    public static final PacketCodec<PacketByteBuf, PurchaseC2SPayload> CODEC =
            PacketCodec.of((payload, buf) -> {
                buf.writeString(payload.shopId());
                buf.writeString(payload.itemId());
                buf.writeVarInt(payload.amount());
            }, buf -> new PurchaseC2SPayload(buf.readString(), buf.readString(), buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
