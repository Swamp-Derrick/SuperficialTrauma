package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.Optional;
import java.util.function.Supplier;

public final class BodyStateCapability {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES, SuperficialTrauma.MOD_ID);
    public static final Supplier<AttachmentType<BodyState>> INSTANCE = ATTACHMENTS.register(
            "body_state", () -> AttachmentType.serializable(BodyState::new).build());
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            SuperficialTrauma.MOD_ID,
            "body_state"
    );

    private BodyStateCapability() {
    }

    public static Optional<BodyState> get(Player player) {
        return Optional.of(player.getData(INSTANCE));
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
