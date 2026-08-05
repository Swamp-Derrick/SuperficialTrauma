package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientInspectionState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record InspectionSnapshotS2CPacket(
        int targetEntityId,
        Component targetName,
        float health,
        float maximumHealth,
        CompoundTag bodyStateTag,
        boolean openScreen
) {
    public InspectionSnapshotS2CPacket {
        targetName = targetName.copy();
        bodyStateTag = bodyStateTag.copy();
    }

    public static void encode(InspectionSnapshotS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeComponent(packet.targetName);
        buffer.writeFloat(packet.health);
        buffer.writeFloat(packet.maximumHealth);
        buffer.writeNbt(packet.bodyStateTag);
        buffer.writeBoolean(packet.openScreen);
    }

    public static InspectionSnapshotS2CPacket decode(FriendlyByteBuf buffer) {
        int targetEntityId = buffer.readVarInt();
        Component targetName = buffer.readComponent();
        float health = buffer.readFloat();
        float maximumHealth = buffer.readFloat();
        CompoundTag bodyStateTag = buffer.readNbt();
        boolean openScreen = buffer.readBoolean();
        return new InspectionSnapshotS2CPacket(
                targetEntityId,
                targetName,
                health,
                maximumHealth,
                bodyStateTag == null ? new CompoundTag() : bodyStateTag,
                openScreen
        );
    }

    public static void handle(InspectionSnapshotS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientInspectionState.update(
                        packet.targetEntityId,
                        packet.targetName,
                        packet.health,
                        packet.maximumHealth,
                        packet.bodyStateTag,
                        packet.openScreen
                )
        ));
        context.setPacketHandled(true);
    }
}
