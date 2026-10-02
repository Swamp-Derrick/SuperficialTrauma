package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.TreatmentService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Closing the surgery screen cancels only the sender's graft, including a start still in transit. */
public record CancelSkinGraftC2SPacket() implements CustomPacketPayload {
    public static final Type<CancelSkinGraftC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "cancel_skin_graft_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelSkinGraftC2SPacket> STREAM_CODEC = StreamCodec.ofMember(CancelSkinGraftC2SPacket::write, CancelSkinGraftC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<CancelSkinGraftC2SPacket> type() { return TYPE; }

    public static void encode(CancelSkinGraftC2SPacket packet, RegistryFriendlyByteBuf buffer) { }
    public static CancelSkinGraftC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CancelSkinGraftC2SPacket();
    }
    public static void handle(CancelSkinGraftC2SPacket packet, IPayloadContext context) {
        var sender = (context.player() instanceof net.minecraft.server.level.ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> TreatmentService.cancelSkinGraft(sender));
        }
    }
}
