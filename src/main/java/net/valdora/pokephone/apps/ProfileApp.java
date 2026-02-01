package net.valdora.pokephone.apps;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.pokephone.App;
import net.valdora.pokephone.appscreens.ProfileAppScreen;

public class ProfileApp implements App {
    @Override
    public Text getName() {
        return Text.literal("Profile");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.of(Valdora.MOD_ID, "textures/gui/apps/profile_icon.png");
    }

    @Override
    public void onOpen(PlayerEntity player) {
        MinecraftClient.getInstance().setScreen(new ProfileAppScreen(Text.literal("Your Profile")));
    }
}
