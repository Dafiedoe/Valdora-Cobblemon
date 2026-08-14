package net.valdora.savedata.flaggedbarrier;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PlayerFlagsS2CPayload(UUID uuid, Map<String, String> flags) implements CustomPayload {
    public static final Identifier ID = Identifier.of("valdora", "player_flags_sync");
    public static final CustomPayload.Id<PlayerFlagsS2CPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);
    
    public static final PacketCodec<RegistryByteBuf, PlayerFlagsS2CPayload> CODEC = PacketCodec.of((payload, buf) -> {
                buf.writeUuid(payload.uuid());
                Map<String, String> map = payload.flags();
                buf.writeInt(map.size());
                for (Map.Entry<String, String> e : map.entrySet()) {
                    buf.writeString(e.getKey());
                    buf.writeString(e.getValue());
                }
            }, (buf) -> {
                UUID uuid = buf.readUuid();
                int size = buf.readInt();
                Map<String, String> map = new HashMap<>(Math.max(4, size));
                for (int i = 0; i < size; i++) {
                    String k = buf.readString(32767);
                    String v = buf.readString(32767);
                    map.put(k, v);
                }
                return new PlayerFlagsS2CPayload(uuid, map);
            }
    );
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
