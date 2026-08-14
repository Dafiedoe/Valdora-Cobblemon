package net.valdora.quests.hud;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

public record QuestHudPayload(String questTitle, String objectiveTitle, int curCount, int reqCount) implements CustomPayload {
    public static final Id<QuestHudPayload> ID = new Id<>(Identifier.of(Valdora.MOD_ID, "quest_hud"));
    public static final PacketCodec<RegistryByteBuf, QuestHudPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, QuestHudPayload::questTitle, PacketCodecs.STRING, QuestHudPayload::objectiveTitle,
            PacketCodecs.INTEGER, QuestHudPayload::curCount, PacketCodecs.INTEGER, QuestHudPayload::reqCount, QuestHudPayload::new);
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
