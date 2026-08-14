package net.valdora.pokephone;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public record PokePhoneRequestProgressPayload(String playerUuid) implements CustomPayload {
    public static final Id<PokePhoneRequestProgressPayload> ID = new Id<>(Identifier.of(Valdora.MOD_ID, "pokephone_request_progress"));
    public static final PacketCodec<RegistryByteBuf, PokePhoneRequestProgressPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, PokePhoneRequestProgressPayload::playerUuid, PokePhoneRequestProgressPayload::new);
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
