package com.minenash.walk_jog_run.mixin;

import com.minenash.walk_jog_run.WalkJogRunClient;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlsOptionsScreen.class)
public class ControlsOptionsScreenMixin {

    @Shadow private @Nullable OptionListWidget optionListWidget;

    @Inject(method = "init", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/gui/widget/OptionListWidget;addAll([Lnet/minecraft/client/option/SimpleOption;)V"))
    public void addStrollingButton(CallbackInfo ci) {
        optionListWidget.addAll(WalkJogRunClient.STROLLING_TOGGLE);
    }

}
