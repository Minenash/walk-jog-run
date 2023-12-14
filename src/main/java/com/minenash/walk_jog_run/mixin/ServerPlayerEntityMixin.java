package com.minenash.walk_jog_run.mixin;

import com.minenash.walk_jog_run.WalkJogRun;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @ModifyArg(method = "increaseTravelMotionStats", at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/server/network/ServerPlayerEntity;addExhaustion(F)V"))
    public float walkJogRun$removeSprintExhaustionFromSprinting(float exhaustion) {
        return 0.0F;
    }

    @Redirect(method = "increaseTravelMotionStats", at = @At(value = "INVOKE", ordinal = 6, target = "Lnet/minecraft/server/network/ServerPlayerEntity;increaseStat(Lnet/minecraft/util/Identifier;I)V"))
    public void walkJogRun$increaseStrollingStat(ServerPlayerEntity player, Identifier stat, int amount) {

        if (WalkJogRun.strolling.getOrDefault(player, false))
            player.increaseStat(WalkJogRun.STROLL_ONE_CM, amount);
        else
            player.increaseStat(stat, amount);

    }

}
