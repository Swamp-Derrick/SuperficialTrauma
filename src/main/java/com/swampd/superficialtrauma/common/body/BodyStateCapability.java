package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public final class BodyStateCapability {
    public static final Capability<BodyState> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            SuperficialTrauma.MOD_ID,
            "body_state"
    );

    private BodyStateCapability() {
    }

    public static LazyOptional<BodyState> get(Player player) {
        return player.getCapability(INSTANCE);
    }
}
