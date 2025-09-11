package net.valdora.savedata.flaggedbarrier;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayerFlagCache {
    private ClientPlayerFlagCache() {}

    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> CACHE = new ConcurrentHashMap<>();

    public static void setFlags(UUID playerUuid, Map<String, String> flags) {
        if (playerUuid == null) return;
        if (flags == null || flags.isEmpty()) {
            CACHE.remove(playerUuid);
            return;
        }
        ConcurrentHashMap<String, String> copy = new ConcurrentHashMap<>(flags);
        CACHE.put(playerUuid, copy);
    }

    public static Map<String, String> getFlags(UUID playerUuid) {
        if (playerUuid == null) return Collections.emptyMap();
        Map<String, String> m = CACHE.get(playerUuid);
        return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
    }

    public static void clearFlags(UUID playerUuid) {
        if (playerUuid == null) return;
        CACHE.remove(playerUuid);
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
