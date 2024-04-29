package com.minenash.walk_jog_run.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncConfigS2CPacket(String json) implements CustomPayload {
    public static final Id<SyncConfigS2CPacket> ID = new Id<>(new Identifier("walk-jog-run", "sync_config"));
    public static final PacketCodec<RegistryByteBuf, SyncConfigS2CPacket> CODEC = PacketCodecs.STRING.xmap(SyncConfigS2CPacket::new, SyncConfigS2CPacket::json).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
