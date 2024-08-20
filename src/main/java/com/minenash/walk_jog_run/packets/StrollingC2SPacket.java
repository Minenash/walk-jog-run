package com.minenash.walk_jog_run.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StrollingC2SPacket(boolean isStrolling) implements CustomPayload {
    public static final Id<StrollingC2SPacket> ID = new Id<>(Identifier.of("walk-jog-run", "strolling"));
    public static final PacketCodec<RegistryByteBuf, StrollingC2SPacket> CODEC = PacketCodecs.BOOL.xmap(StrollingC2SPacket::new, StrollingC2SPacket::isStrolling).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
