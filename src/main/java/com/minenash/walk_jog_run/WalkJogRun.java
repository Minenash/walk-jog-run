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

	private static final UUID BASE_SPEED_MODIFIER_ID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278C");
	private static final EntityAttributeModifier BASE_SPEED_MODIFIER;
	private static final boolean USE_BASE_SPEED_MODIFIER;

	private static final UUID STROLLING_SPEED_MODIFIER_ID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278E");
	private static final EntityAttributeModifier STROLLING_SPEED_MODIFIER;
	public static final Identifier STROLL_ONE_CM = id("stroll_one_cm");

	public static final Map<PlayerEntity, Boolean> strolling = new HashMap<>();
	public static final Map<PlayerEntity, Integer> stamina = new HashMap<>();

	static {
		Config.read();
		STROLLING_SPEED_MODIFIER = new EntityAttributeModifier(STROLLING_SPEED_MODIFIER_ID, "WalkJogRun: Strolling speed modification",
				Config.STROLLING_SPEED_MODIFIER, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		BASE_SPEED_MODIFIER = new EntityAttributeModifier(BASE_SPEED_MODIFIER_ID, "WalkJogRun: Base speed modification",
				Config.BASE_WALKING_SPEED_MODIFIER, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		USE_BASE_SPEED_MODIFIER = Config.BASE_WALKING_SPEED_MODIFIER != 0;
	}

    //TODO: Test on Server

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

				if (USE_BASE_SPEED_MODIFIER) {
					EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
					if (instance != null && !instance.hasModifier(BASE_SPEED_MODIFIER))
						instance.addTemporaryModifier(BASE_SPEED_MODIFIER);
				}

				int max_stamina = player.getHungerManager().getFoodLevel() * Config.STAMINA_PER_FOOD_LEVEL;
				int player_stamina = stamina.getOrDefault(player, max_stamina);

				if (player.isSprinting())
					player_stamina -= Config.STAMINA_DEPLETION_PER_TICK;
				else
					player_stamina += strolling.getOrDefault(player, false) ? Config.STAMINA_RECOVERY_STROLLING : Config.STAMINA_RECOVERY_WALKING;

				if (player_stamina < 0) {
					player.setSprinting(false);
					player.addStatusEffect( new StatusEffectInstance(StatusEffects.SLOWNESS, Config.STAMINA_EXHAUSTED_SLOWNESS_DURATION_IN_TICKS, 0, false, Config.STAMINA_EXHAUSTED_SLOWNESS_SHOW_PARTICLES));
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
