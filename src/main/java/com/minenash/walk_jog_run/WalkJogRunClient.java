package com.minenash.walk_jog_run;

import com.minenash.walk_jog_run.config.ClientConfig;
import com.minenash.walk_jog_run.config.ServerConfig;
import com.minenash.walk_jog_run.packets.StaminaS2CPacket;
import com.minenash.walk_jog_run.packets.StrollingC2SPacket;
import com.minenash.walk_jog_run.packets.SyncConfigS2CPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.StickyKeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.UUID;

public class WalkJogRunClient implements ClientModInitializer {

    public static final SimpleOption<Boolean> STROLLING_TOGGLE = new SimpleOption<>("walkjogrun.keybind.strolling", SimpleOption.emptyTooltip(),
            (optionText, value) -> value ? Text.translatable("options.key.toggle") : Text.translatable("options.key.hold"),
            SimpleOption.BOOLEAN, true, (value) -> {});

    public static final KeyBinding STROLLING_KEYBIND = new StickyKeyBinding("walkjogrun.keybind.strolling", GLFW.GLFW_KEY_LEFT_ALT, KeyBinding.MOVEMENT_CATEGORY, STROLLING_TOGGLE::getValue);

    public static boolean isStrolling = false;
    public static boolean wasSprinting = false;
    public static boolean wasStrolling = false;

    public static int stamina = 200;

    private static final Identifier STROLLING_TEXTURE = WalkJogRun.id("textures/gui/strolling.png");
    private static final Identifier WALKING_TEXTURE = WalkJogRun.id("textures/gui/walking.png");
    private static final Identifier SPRINTING_TEXTURE = WalkJogRun.id("textures/gui/sprinting.png");
    private static final Identifier STROLLING_FILL_TEXTURE = WalkJogRun.id("textures/gui/strolling_fill.png");
    private static final Identifier WALKING_FILL_TEXTURE = WalkJogRun.id("textures/gui/walking_fill.png");
    private static final Identifier SPRINTING_FILL_TEXTURE = WalkJogRun.id("textures/gui/sprinting_fill.png");
    private static final Identifier HUNGER_STAMINA_TEXTURE = WalkJogRun.id("textures/gui/hunger_stamina.png");

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static int[] hungerBarStaminaYValues = new int[10];
    public static float[] hungerBarStaminaColor = Color.decode(ClientConfig.hungerBarStaminaColor).getRGBComponents(null);

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(STROLLING_KEYBIND);

        ClientConfig.init("walk-jog-run-client", ClientConfig.class);

        ClientTickEvents.END_WORLD_TICK.register(client -> {

            if (isSprinting() != wasSprinting) {
                if (wasSprinting)
                    setStrolling(wasStrolling);
                else {
                    wasStrolling = isStrolling;
                    setStrolling(false);
                }
                wasSprinting = isSprinting();
                return;
            }
            wasSprinting = isSprinting();

            if (STROLLING_TOGGLE.getValue())
                while (!isSprinting() && STROLLING_KEYBIND.wasPressed())
                    setStrolling(isStrolling = !isStrolling);
            else if (!isSprinting() && isStrolling != STROLLING_KEYBIND.isPressed())
                setStrolling(isStrolling = STROLLING_KEYBIND.isPressed());


        });

        ClientPlayNetworking.registerGlobalReceiver(StaminaS2CPacket.ID, (payload, context) -> {
            stamina = payload.stamina();
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncConfigS2CPacket.ID, (payload, context) -> {
            ServerConfig.applyConfig(payload.json());
        });

        ClientPlayConnectionEvents.DISCONNECT.register(WalkJogRun.id("sync_correct"), (handler, client1) -> {
            ServerConfig.read();
        });

        HudRenderCallback.EVENT.register( WalkJogRun.id("icon_render"), WalkJogRunClient::render);

    }

    private static void render(DrawContext context, RenderTickCounter tick) {

        if (client.player.isSpectator())
            return;
        int y = getIconY();
        int x = getIconX();
        int size = ClientConfig.iconPosition == ClientConfig.IconPosition.CROSSHAIR || ClientConfig.iconPosition == ClientConfig.IconPosition.ABOVE_HOTBAR? 10 : 16;
        int max_stamina = client.player.getHungerManager().getFoodLevel() * ServerConfig.STAMINA_PER_FOOD_LEVEL;

        context.getMatrices().translate(0,0, 60);


        if (stamina > 0 || !ServerConfig.STAMINA_ENABLED)
            context.drawTexture(isSprinting() ? SPRINTING_TEXTURE : isStrolling ? STROLLING_TEXTURE : WALKING_TEXTURE,
                x, y, size, size, 0, 0, 20, 20, 20, 20);

        if ( ServerConfig.STAMINA_ENABLED && (stamina < max_stamina || max_stamina == 0) && !client.player.isCreative()) {
            if (ClientConfig.showStaminaInIcon) {

                int height = size - (int) (1F * size * stamina / max_stamina);
                context.drawTexture(isSprinting() ? SPRINTING_FILL_TEXTURE : isStrolling ? STROLLING_FILL_TEXTURE : WALKING_FILL_TEXTURE,
                        x, y, size, height, 0, 0, 20, (int) (20F * height / size), 20, 20);
            }

            if (ClientConfig.showStaminaInHungerBar)
                renderHungerBarStamina(context);
        }
        context.getMatrices().translate(0,0, -60);
    }

    private static void renderHungerBarStamina(DrawContext context) {

        RenderSystem.enableBlend();
//        RenderSystem.setShaderTexture(0, HUNGER_STAMINA_TEXTURE);
        RenderSystem.setShaderColor(hungerBarStaminaColor[0], hungerBarStaminaColor[1], hungerBarStaminaColor[2], 1F);


        int x = client.getWindow().getScaledWidth() / 2 + 91 - 9;
        double s = stamina / 8.888;

        if (ClientConfig.hungerBarColorState == ClientConfig.HungerBarColorState.STAMINA_DEPLETED) {
            for (int x2 = 0; x2 < 10; ++x2) {
                if (s <= x2 * 9 + 9) {
                    int ss = (int) (x2 * 9 + 9 - s);
                    context.drawTexture(HUNGER_STAMINA_TEXTURE, x - x2 * 8, hungerBarStaminaYValues[x2], 0, 0, ss > 8 ? 9 : ss, 9, 9, 9);
                }
            }
        }
        else {
            for (int x2 = 0; x2 < 10; ++x2) {
                if (s > x2*9) {
                    int ss = (int) (s - x2*9);

                    if (ss > 8)
                        context.drawTexture(HUNGER_STAMINA_TEXTURE, x - x2 * 8, hungerBarStaminaYValues[x2], 0, 0, 9, 9, 9, 9);
                    else
                        context.drawTexture(HUNGER_STAMINA_TEXTURE, x - x2 * 8 + (9 - ss), hungerBarStaminaYValues[x2], 9 - ss, 0, ss, 9, 9, 9);
                }
            }
        }
        RenderSystem.setShaderColor(1,1,1, 1F);
        RenderSystem.disableBlend();

    }

    private static int getIconY() {
        int height = client.getWindow().getScaledHeight();
        return switch (ClientConfig.iconPosition) {
            case HOTBAR -> height - 20 + 2;
            case ABOVE_HOTBAR -> height - 46;
            case CROSSHAIR -> height / 2 + 1;
            case TOP_LEFT_CORNER, TOP_RIGHT_CORNER -> 5;
            case BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER -> height - 20;
        };
    }

    private static int getIconX() {
        int width = client.getWindow().getScaledWidth();
        return switch (ClientConfig.iconPosition) {
            case HOTBAR -> width / 2 + (client.player.getMainArm() == Arm.RIGHT ? 92 : -110) + 2;
            case ABOVE_HOTBAR -> width / 2 - 5;
            case CROSSHAIR -> width / 2 + 1;
            case TOP_LEFT_CORNER, BOTTOM_LEFT_CORNER -> 5;
            case TOP_RIGHT_CORNER, BOTTOM_RIGHT_CORNER -> width - 20;
        };
    }


    private static final Identifier SPRINTING_MODIFIER_ID = Identifier.ofVanilla("sprinting");
    private static boolean isSprinting() {
        EntityAttributeInstance instance = client.player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        return instance != null && instance.getModifier(SPRINTING_MODIFIER_ID) != null;
    }

    private static void setStrolling(boolean strolling) {
        ClientPlayNetworking.send( new StrollingC2SPacket(strolling));
    }
}
