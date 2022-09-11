package com.minenash.walk_jog_run.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor @Mutable
    static void setSPRINTING_SPEED_BOOST(EntityAttributeModifier modifier) {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static UUID getSPRINTING_SPEED_BOOST_ID() {
        throw new UnsupportedOperationException();
    }
}
