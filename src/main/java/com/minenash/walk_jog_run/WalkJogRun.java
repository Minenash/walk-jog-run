package com.minenash.walk_jog_run;

import com.minenash.walk_jog_run.config.ServerConfig;
import com.minenash.walk_jog_run.mixin.LivingEntityAccessor;
import com.minenash.walk_jog_run.packets.StaminaS2CPacket;
import com.minenash.walk_jog_run.packets.StrollingC2SPacket;
import com.minenash.walk_jog_run.packets.SyncConfigS2CPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.minecraft.server.command.CommandManager.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WalkJogRun implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("walk-jog-run");

	private static final Identifier BASE_SPEED_MODIFIER_ID = Identifier.of("walk-jog-run", "base_speed");
	private static EntityAttributeModifier BASE_SPEED_MODIFIER;

	private static final Identifier STROLLING_SPEED_MODIFIER_ID = Identifier.of("walk-jog-run", "strolling_speed");
	private static EntityAttributeModifier STROLLING_SPEED_MODIFIER;
	public static final Identifier STROLL_ONE_CM = id("stroll_one_cm");

	public static final Map<PlayerEntity, Boolean> strolling = new HashMap<>();
	public static final Map<PlayerEntity, Integer> stamina = new HashMap<>();

	public static String SERVER_CONFIG_JSON = "";

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(StaminaS2CPacket.ID, StaminaS2CPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncConfigS2CPacket.ID, SyncConfigS2CPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(StrollingC2SPacket.ID, StrollingC2SPacket.CODEC);

		ServerConfig.read();
		updateModifiers();


		Registry.register(Registries.CUSTOM_STAT, "stroll_one_cm", STROLL_ONE_CM);
		Stats.CUSTOM.getOrCreateStat(STROLL_ONE_CM, StatFormatter.DISTANCE);



		ServerPlayNetworking.registerGlobalReceiver( StrollingC2SPacket.ID, (payload, context) -> {
			EntityAttributeInstance movement = context.player().getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			boolean strollingP = payload.isStrolling();
			strolling.put(context.player(), strollingP);

			if (strollingP) {
				if (!movement.hasModifier(STROLLING_SPEED_MODIFIER_ID))
					movement.addTemporaryModifier(STROLLING_SPEED_MODIFIER);
			}
			else
				movement.removeModifier(STROLLING_SPEED_MODIFIER);

		});

		ServerTickEvents.START_SERVER_TICK.register(id("stamina"), server -> {
			if (!ServerConfig.STAMINA_ENABLED)
				return;
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

				EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
				if (instance != null) {
					if (!instance.hasModifier(BASE_SPEED_MODIFIER_ID)) {
						instance.addTemporaryModifier(BASE_SPEED_MODIFIER);
					}
					else if (instance.getModifier(BASE_SPEED_MODIFIER_ID).value() != BASE_SPEED_MODIFIER.value()) {
						instance.removeModifier(BASE_SPEED_MODIFIER_ID);
						instance.addTemporaryModifier(BASE_SPEED_MODIFIER);
					}
				}

				int max_stamina = player.getHungerManager().getFoodLevel() * ServerConfig.STAMINA_PER_FOOD_LEVEL;
				int player_stamina = stamina.getOrDefault(player, max_stamina);

				if (player.isSprinting() && !player.isCreative() && !player.isSpectator())
					player_stamina -= ServerConfig.STAMINA_DEPLETION_PER_TICK;
				else
					player_stamina += strolling.getOrDefault(player, false) ? ServerConfig.STAMINA_RECOVERY_STROLLING : ServerConfig.STAMINA_RECOVERY_WALKING;

				player_stamina = MathHelper.clamp(player_stamina, 0, max_stamina);
				setStamina(player, player_stamina);

				if (player_stamina == 0) {
					player.setSprinting(false);
					player.addStatusEffect( new StatusEffectInstance(StatusEffects.SLOWNESS, ServerConfig.STAMINA_EXHAUSTED_SLOWNESS_DURATION_IN_TICKS, 0, false, ServerConfig.STAMINA_EXHAUSTED_SLOWNESS_SHOW_PARTICLES));
				}


			}
		});

		ServerPlayConnectionEvents.JOIN.register(id("sync_config"), (handler, sender, server) -> {
			sender.sendPacket(new SyncConfigS2CPacket(SERVER_CONFIG_JSON));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("walkjogrun").then(literal("reload")
				.executes(context -> {
					ServerConfig.read();
					updateModifiers();
					context.getSource().getServer().getPlayerManager()
							.sendToAll( ServerPlayNetworking.createS2CPacket( new SyncConfigS2CPacket(SERVER_CONFIG_JSON) ) );

					context.getSource().sendMessage(Text.literal("Walk Jog Run: Config reloaded"));
					return 1;
				})
			));
		});

	}

	public static void updateModifiers() {
		LivingEntityAccessor.setSPRINTING_SPEED_BOOST(new EntityAttributeModifier(LivingEntityAccessor.getSPRINTING_SPEED_BOOST().id(), ServerConfig.SPRINTING_SPEED_MODIFIER, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

		STROLLING_SPEED_MODIFIER = new EntityAttributeModifier(STROLLING_SPEED_MODIFIER_ID,
				ServerConfig.STROLLING_SPEED_MODIFIER, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		BASE_SPEED_MODIFIER = new EntityAttributeModifier(BASE_SPEED_MODIFIER_ID,
				ServerConfig.BASE_WALKING_SPEED_MODIFIER, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	private void setStamina(ServerPlayerEntity player, int staminaP) {
		stamina.put(player, staminaP);
		ServerPlayNetworking.send(player, new StaminaS2CPacket(staminaP));
	}

	public static Identifier id(String str) {
		return Identifier.of("walk-jog-run", str);
	}

}
