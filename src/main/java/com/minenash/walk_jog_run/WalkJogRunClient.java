package com.minenash.walk_jog_run;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class WalkJogRunClient implements ClientModInitializer {

    public static final KeyBinding STROLLING_KEYBIND = keybind("strolling", GLFW.GLFW_KEY_TAB);

    private static KeyBinding keybind(String key, int defaultKey) {
        KeyBinding binding = new KeyBinding("walkjogrun.keybind." + key, InputUtil.Type.KEYSYM, defaultKey, KeyBinding.MOVEMENT_CATEGORY);
        KeyBindingHelper.registerKeyBinding(binding);
        return binding;
    }

    public static boolean isStrolling = false;
    boolean wasSprinting = false;
    boolean wasStrolling = false;

    int stamina = 200;

    private static final Identifier STROLLING_TEXTURE = WalkJogRun.id("textures/gui/strolling.png");
    private static final Identifier WALKING_TEXTURE = WalkJogRun.id("textures/gui/walking.png");
    private static final Identifier SPRINTING_TEXTURE = WalkJogRun.id("textures/gui/sprinting.png");
    private static final Identifier STROLLING_FILL_TEXTURE = WalkJogRun.id("textures/gui/strolling_fill.png");
    private static final Identifier WALKING_FILL_TEXTURE = WalkJogRun.id("textures/gui/walking_fill.png");
    private static final Identifier SPRINTING_FILL_TEXTURE = WalkJogRun.id("textures/gui/sprinting_fill.png");

    private static final MinecraftClient client = MinecraftClient.getInstance();

    @Override
    public void onInitializeClient() {

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

        HudRenderCallback.EVENT.register( WalkJogRun.id("icon_render"), (matrix, tickDelta) -> {
            matrix.push();

            int y = client.getWindow().getScaledHeight() - 20;
            int x = client.getWindow().getScaledWidth() / 2 + (client.player.getMainArm() == Arm.RIGHT ? 92 : -110);
            int max_stamina = client.player.getHungerManager().getFoodLevel() * 40;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableDepthTest();

            RenderSystem.setShaderTexture(0, isSprinting() ? SPRINTING_TEXTURE : isStrolling ? STROLLING_TEXTURE : WALKING_TEXTURE);
            DrawableHelper.drawTexture(matrix, x, y, 20, 20, 0, 0, 100, 100, 100, 100);

            if (stamina < max_stamina) {
                RenderSystem.setShaderTexture(0, isSprinting() ? SPRINTING_FILL_TEXTURE : isStrolling ? STROLLING_FILL_TEXTURE : WALKING_FILL_TEXTURE);

                int height = 20 - (int) (20F*stamina/max_stamina);
                DrawableHelper.drawTexture(matrix, x, y, 20, height, 0, 0, 100, (int)(height/20F*100), 100, 100);
            }

            DrawableHelper.drawTextWithShadow(matrix, client.textRenderer, Text.literal("Stamina: §e" + stamina), 5, 5, 0xFFFFFF);

            matrix.pop();

        });

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
