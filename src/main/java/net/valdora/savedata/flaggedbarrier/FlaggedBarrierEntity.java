package net.valdora.savedata.flaggedbarrier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.valdora.general.ModBlockEntities;

import java.util.Locale;

public class FlaggedBarrierEntity extends BlockEntity {
    private String flagName = "";
    private String flagValue = "";

    public FlaggedBarrierEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLAGGED_BARRIER_ENTITY, pos, state);
    }

    public String getFlagName() { return flagName; }
    public String getFlagValue() { return flagValue; }

    public void setFlagData(String name, String value) {
        this.flagName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        this.flagValue = value == null ? "" : value.toLowerCase(Locale.ROOT);

        markDirty();

        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putString("FlagName", flagName);
        nbt.putString("FlagValue", flagValue);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        flagName = nbt.getString("FlagName");
        flagValue = nbt.getString("FlagValue");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbtWithIdentifyingData(lookup);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
