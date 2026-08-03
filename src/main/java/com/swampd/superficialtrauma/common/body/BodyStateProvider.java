package com.swampd.superficialtrauma.common.body;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BodyStateProvider implements ICapabilitySerializable<CompoundTag> {
    private final BodyState bodyState = new BodyState();
    private final LazyOptional<BodyState> optional = LazyOptional.of(() -> bodyState);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability,
            @Nullable Direction side
    ) {
        return capability == BodyStateCapability.INSTANCE ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return bodyState.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        bodyState.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
