package net.valdora.general;

import com.cobblemon.mod.common.Cobblemon;
import net.minecraft.server.network.ServerPlayerEntity;

public class PokemonPartyApi {

    public static boolean hasSurf(ServerPlayerEntity player) {
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        return party.toGappyList().stream()
                .filter(x -> x != null)
                .anyMatch(pokemon ->
                        pokemon.getMoveSet().getMoves().stream()
                                .anyMatch(move ->
                                        move.getTemplate().getName().equalsIgnoreCase("surf")
                                )
                );
    }
}
