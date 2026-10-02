package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientInspectionState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record InspectionSnapshotS2CPacket(
        int targetEntityId,
        Component targetName,
        float health,
        float maximumHealth,
        CompoundTag bodyStateTag,
        boolean openScreen
) implements CustomPacketPayload {
    public static final Type<InspectionSnapshotS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "inspection_snapshot_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InspectionSnapshotS2CPacket> STREAM_CODEC = StreamCodec.ofMember(InspectionSnapshotS2CPacket::write, InspectionSnapshotS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<InspectionSnapshotS2CPacket> type() { return TYPE; }

    public InspectionSnapshotS2CPacket {
        targetName = targetName.copy();
        bodyStateTag = bodyStateTag.copy();
    }

    public static void encode(InspectionSnapshotS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        ComponentSerialization.STREAM_CODEC.encode(buffer, packet.targetName);
        buffer.writeFloat(packet.health);
        buffer.writeFloat(packet.maximumHealth);
        buffer.writeNbt(packet.bodyStateTag);
        buffer.writeBoolean(packet.openScreen);
    }

    public static InspectionSnapshotS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        int targetEntityId = buffer.readVarInt();
        Component targetName = ComponentSerialization.STREAM_CODEC.decode(buffer);
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

    public static void handle(InspectionSnapshotS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientInspectionState.update(
                        packet.targetEntityId,
                        packet.targetName,
                        packet.health,
                        packet.maximumHealth,
                        packet.bodyStateTag,
                        packet.openScreen
                )
        );
    }
}
