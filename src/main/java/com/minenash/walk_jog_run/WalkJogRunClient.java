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
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.lwjgl.glfw.GLFW;

public class WalkJogRunClient implements ClientModInitializer {

    public static final KeyBinding STROLLING_KEYBIND = keybind("strolling", GLFW.GLFW_KEY_LEFT_ALT);

    private static KeyBinding keybind(String key, int defaultKey) {
        KeyBinding binding = new KeyBinding("walkjogrun.keybind." + key, InputUtil.Type.KEYSYM, defaultKey, KeyBinding.MOVEMENT_CATEGORY);
        KeyBindingHelper.registerKeyBinding(binding);
        return binding;
    }

    public static boolean isStrolling = false;
    boolean wasSprinting = false;
    boolean wasStrolling = false;

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

        HudRenderCallback.EVENT.register( WalkJogRun.id("icon_render"), (matrix, tickDelta) -> {
            matrix.push();

            int y = getIconY();
            int x = getIconX();
            int size = ClientConfig.iconPosition == ClientConfig.IconPosition.CROSSHAIR ? 10 : 16;
            int max_stamina = client.player.getHungerManager().getFoodLevel() * ServerConfig.STAMINA_PER_FOOD_LEVEL;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableDepthTest();

            RenderSystem.setShaderTexture(0, isSprinting() ? SPRINTING_TEXTURE : isStrolling ? STROLLING_TEXTURE : WALKING_TEXTURE);
            DrawableHelper.drawTexture(matrix, x, y, size, size, 0, 0, 80, 80, 80, 80);

            if (stamina < max_stamina && !client.player.isCreative()) {
                if (ClientConfig.showStaminaInIcon) {
                    RenderSystem.setShaderTexture(0, isSprinting() ? SPRINTING_FILL_TEXTURE : isStrolling ? STROLLING_FILL_TEXTURE : WALKING_FILL_TEXTURE);

                    int height = size - (int) (1F * size * stamina / max_stamina);
                    DrawableHelper.drawTexture(matrix, x, y, size, height, 0, 0, 80, (int) (80F * height / size), 80, 80);
                }

                if (ClientConfig.showStaminaInHungerBar) {
                    RenderSystem.setShaderTexture(0, HUNGER_STAMINA_TEXTURE);

                    int n = client.getWindow().getScaledWidth() / 2 + 91 - 9;

                    double s = stamina / 8.888;

                    for (int x2 = 0; x2 < 10; ++x2) {
//                    if (s > x2*9) {
//                        int ss = (int) (s - x2*9);
//
//                        if (ss > 8)
//                            DrawableHelper.drawTexture(matrix, n - x2 * 8, o, 0, 0, 9, 9, 9, 9);
//                        else
//                            DrawableHelper.drawTexture(matrix, n - x2 * 8 + (9 - ss), o, 9 - ss, 0, ss, 9, 9, 9);
//                    }
                        if (s <= x2 * 9 + 9) {
                            int ss = (int) (x2 * 9 + 9 - s);
                            DrawableHelper.drawTexture(matrix, n - x2 * 8, hungerBarStaminaYValues[x2], 0, 0, ss > 8 ? 9 : ss, 9, 9, 9);
                        }
                    }
                }

            }

//            DrawableHelper.drawTextWithShadow(matrix, client.textRenderer, Text.literal("Stamina: §e" + stamina), 5, 5, 0xFFFFFF);

            matrix.pop();

        });

    }

    private int getIconY() {
        int height = client.getWindow().getScaledHeight();
        return switch (ClientConfig.iconPosition) {
            case HOTBAR -> height - 20 + 2;
            case CROSSHAIR -> height / 2 + 1;
            case TOP_LEFT_CORNER, TOP_RIGHT_CORNER -> 5;
            case BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER -> height - 20;
        };
    }

    private int getIconX() {
        int width = client.getWindow().getScaledWidth();
        return switch (ClientConfig.iconPosition) {
            case HOTBAR -> width / 2 + (client.player.getMainArm() == Arm.RIGHT ? 92 : -110) + 2;
            case CROSSHAIR -> width / 2 + 1;
            case TOP_LEFT_CORNER, BOTTOM_LEFT_CORNER -> 5;
            case TOP_RIGHT_CORNER, BOTTOM_RIGHT_CORNER -> width - 20;
        };
    }


    private boolean isSprinting() {
        return client.player.isSprinting();
    }

    private void setStrolling(boolean strolling) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(strolling);
        ClientPlayNetworking.send( WalkJogRun.id("strolling"), buf);
    }
}
