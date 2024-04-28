package com.minenash.walk_jog_run.mixin;

import com.minenash.walk_jog_run.WalkJogRunClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Inject(method = "accept", at = @At("HEAD"))
    public void addMoreOptions(GameOptions.Visitor visitor, CallbackInfo info) {
        visitor.accept("doubleTapSprint", WalkJogRunClient.STROLLING_TOGGLE);
    }

}
