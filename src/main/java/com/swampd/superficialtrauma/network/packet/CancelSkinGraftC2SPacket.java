package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.TreatmentService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Closing the surgery screen cancels only the sender's graft, including a start still in transit. */
public record CancelSkinGraftC2SPacket() {
    public static void encode(CancelSkinGraftC2SPacket packet, FriendlyByteBuf buffer) { }
    public static CancelSkinGraftC2SPacket decode(FriendlyByteBuf buffer) {
        return new CancelSkinGraftC2SPacket();
    }
    public static void handle(CancelSkinGraftC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        var sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> TreatmentService.cancelSkinGraft(sender));
        }
        context.setPacketHandled(true);
    }
}
