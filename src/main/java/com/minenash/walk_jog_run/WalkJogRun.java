package com.minenash.walk_jog_run;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WalkJogRun implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("walk-jog-run");

	private static final UUID STROLLING_SPEED_MODIFIER_ID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278E");
	private static final EntityAttributeModifier STROLLING_SPEED_MODIFIER = new EntityAttributeModifier(STROLLING_SPEED_MODIFIER_ID, "Strolling speed modification", -0.3f, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
	public static final Identifier STROLL_ONE_CM = id("stroll_one_cm");

	public static final Map<PlayerEntity, Boolean> strolling = new HashMap<>();
	public static final Map<PlayerEntity, Integer> stamina = new HashMap<>();

	@Override
	public void onInitialize() {

		Registry.register(Registry.CUSTOM_STAT, "stroll_one_cm", STROLL_ONE_CM);
		Stats.CUSTOM.getOrCreateStat(STROLL_ONE_CM, StatFormatter.DISTANCE);

		ServerPlayNetworking.registerGlobalReceiver( id("strolling"), (server, player, handler, buf, responseSender) -> {
			EntityAttributeInstance movement = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			boolean strollingP = buf.readBoolean();
			strolling.put(player, strollingP);

			if (strollingP) {
				movement.addTemporaryModifier(STROLLING_SPEED_MODIFIER);
				player.sendMessage(Text.literal("Walking (Strolling) Speed"), true);
			}
			else {
				movement.removeModifier(STROLLING_SPEED_MODIFIER);

				if (!player.isSprinting())
					player.sendMessage(Text.literal("Jogging (Normal) Speed"), true);
			}

		});

		ServerTickEvents.START_SERVER_TICK.register(id("stamina"), server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

				int max_stamina = player.getHungerManager().getFoodLevel() * 40;
				int player_stamina = stamina.getOrDefault(player, max_stamina);

				if (player.isSprinting())
					player_stamina -= 2;
				else
					player_stamina += strolling.getOrDefault(player, false) ? 2 : 1;

				if (player_stamina < 0) {
					player.setSprinting(false);
					player.addStatusEffect( new StatusEffectInstance(StatusEffects.SLOWNESS, 100));
				}

				setStamina(player, MathHelper.clamp(player_stamina, 0, max_stamina));
			}
		});

	}

	private void setStamina(ServerPlayerEntity player, int staminaP) {
		stamina.put(player, staminaP);

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(staminaP);
		ServerPlayNetworking.send(player, WalkJogRun.id("stamina"), buf);
	}

	public static Identifier id(String str) {
		return new Identifier("walk-jog-run", str);
	}

}
