package net.valdora.pokephone;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public record PokePhoneRespondProgressPayload(int badgeCount, int pokedollars) implements CustomPayload {
    public static final Id<PokePhoneRespondProgressPayload> ID = new Id<>(Identifier.of(Valdora.MOD_ID, "pokephone_respond_progress"));
    public static final PacketCodec<RegistryByteBuf, PokePhoneRespondProgressPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, PokePhoneRespondProgressPayload::badgeCount,
            PacketCodecs.INTEGER, PokePhoneRespondProgressPayload::pokedollars, PokePhoneRespondProgressPayload::new);
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}