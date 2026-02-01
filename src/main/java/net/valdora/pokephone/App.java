package net.valdora.pokephone;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public interface App {
    Text getName();
    Identifier getIcon();
    void onOpen(PlayerEntity player);
}
