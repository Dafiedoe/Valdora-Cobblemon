package net.valdora.general;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.savedata.flaggedbarrier.GetBarrierCommand;

public final class ModComponents {
    private ModComponents() {}

    public static final ComponentType<String> FLAG_NAME = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Valdora.MOD_ID, "flag_name"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final ComponentType<String> FLAG_VALUE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Valdora.MOD_ID, "flag_value"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            GetBarrierCommand.register(dispatcher);
        }));
    }
}
