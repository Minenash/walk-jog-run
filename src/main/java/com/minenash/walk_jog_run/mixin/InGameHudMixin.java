package com.minenash.walk_jog_run.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minenash.walk_jog_run.WalkJogRunClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    private static final int OFFSET_LEFT = -18;
    private static final int OFFSET_RIGHT = 16;

    @ModifyArg(method = "renderHotbar", index = 1, at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    public int walkJogRun$changeHotbarAttackIndicatorX(int old) {
        return old + (MinecraftClient.getInstance().player.getMainArm() == Arm.RIGHT ? OFFSET_RIGHT : OFFSET_LEFT);
    }

    @ModifyArg(method = "renderHotbar", index = 1, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIIIIIII)V"))
    public int walkJogRun$changeHotbarAttackIndicatorX2(int old) {
        return old + (MinecraftClient.getInstance().player.getMainArm() == Arm.RIGHT ? OFFSET_RIGHT : OFFSET_LEFT);
    }

    @Inject(method = "renderFood", locals = LocalCapture.CAPTURE_FAILSOFT, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    public void walkJogRun$getHungerBarYValues(DrawContext context, PlayerEntity player, int top, int left, CallbackInfo ci, HungerManager hungerManager, int i, int j, int k, Identifier identifier, Identifier identifier2, Identifier identifier3, int l) {
        WalkJogRunClient.hungerBarStaminaYValues[j] = k;
    }

}
