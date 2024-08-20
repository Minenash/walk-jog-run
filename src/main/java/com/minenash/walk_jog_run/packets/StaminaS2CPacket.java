package com.minenash.walk_jog_run.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StaminaS2CPacket(int stamina) implements CustomPayload {
    public static final Id<StaminaS2CPacket> ID = new Id<>(Identifier.of("walk-jog-run", "stamina"));
    public static final PacketCodec<RegistryByteBuf, StaminaS2CPacket> CODEC = PacketCodecs.VAR_INT.xmap(StaminaS2CPacket::new, StaminaS2CPacket::stamina).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
