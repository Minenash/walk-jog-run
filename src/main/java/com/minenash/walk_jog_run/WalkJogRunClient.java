package com.minenash.walk_jog_run;

import com.minenash.walk_jog_run.config.ClientConfig;
import com.minenash.walk_jog_run.config.ServerConfig;
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
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class WalkJogRunClient implements ClientModInitializer {

    public static final KeyBinding STROLLING_KEYBIND = keybind("strolling", GLFW.GLFW_KEY_LEFT_ALT);

    private static KeyBinding keybind(String key, int defaultKey) {
        KeyBinding binding = new KeyBinding("walkjogrun.keybind." + key, InputUtil.Type.KEYSYM, defaultKey, KeyBinding.MOVEMENT_CATEGORY);
        KeyBindingHelper.registerKeyBinding(binding);
        return binding;
    }

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


            while (!isSprinting() && STROLLING_KEYBIND.wasPressed())
                setStrolling(isStrolling = !isStrolling);
        });

        ClientPlayNetworking.registerGlobalReceiver(WalkJogRun.id("stamina"), (client1, handler, buf, responseSender) -> {
            stamina = buf.readInt();
        });

        ClientPlayNetworking.registerGlobalReceiver(WalkJogRun.id("sync_config"), (client1, handler, buf, responseSender) -> {
            ServerConfig.applyConfig(buf.readString());
        });

        ClientPlayConnectionEvents.DISCONNECT.register(WalkJogRun.id("sync_correct"), (handler, client1) -> {
            ServerConfig.read();
        });

        HudRenderCallback.EVENT.register( WalkJogRun.id("icon_render"), WalkJogRunClient::render);

    }

    private static void render(DrawContext context, float tickDelta) {
        if (client.player.isSpectator())
            return;
        int y = getIconY();
        int x = getIconX();
        int size = ClientConfig.iconPosition == ClientConfig.IconPosition.CROSSHAIR || ClientConfig.iconPosition == ClientConfig.IconPosition.ABOVE_HOTBAR? 10 : 16;
        int max_stamina = client.player.getHungerManager().getFoodLevel() * ServerConfig.STAMINA_PER_FOOD_LEVEL;

//            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
//            RenderSystem.enableDepthTest();

        context.drawTexture(isSprinting() ? SPRINTING_TEXTURE : isStrolling ? STROLLING_TEXTURE : WALKING_TEXTURE,
                x, y, size, size, 0, 0, 20, 20, 20, 20);

        if (stamina < max_stamina && !client.player.isCreative()) {
            if (ClientConfig.showStaminaInIcon) {

                int height = size - (int) (1F * size * stamina / max_stamina);
                context.drawTexture(isSprinting() ? SPRINTING_FILL_TEXTURE : isStrolling ? STROLLING_FILL_TEXTURE : WALKING_FILL_TEXTURE,
                        x, y, size, height, 0, 0, 20, (int) (20F * height / size), 20, 20);
            }

            if (ClientConfig.showStaminaInHungerBar)
                renderHungerBarStamina(context);
        }
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
            case ABOVE_HOTBAR -> height - 50;
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


    private static boolean isSprinting() {
        return client.player.isSprinting();
    }

    private static void setStrolling(boolean strolling) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(strolling);
        ClientPlayNetworking.send( WalkJogRun.id("strolling"), buf);
    }
}
