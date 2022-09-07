package com.minenash.walk_jog_run.mixin;

import com.minenash.walk_jog_run.Config;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @ModifyArg(method = "<clinit>", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/attribute/EntityAttributeModifier;<init>(Ljava/util/UUID;Ljava/lang/String;DLnet/minecraft/entity/attribute/EntityAttributeModifier$Operation;)V"))
    private static double walkJogRun$applyConfigSprintSpeedModifier(double value) {
        Config.read();
        return Config.SPRINTING_SPEED_MODIFIER;
    }

}
