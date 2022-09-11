package com.minenash.walk_jog_run.config;

public class ClientConfig extends MidnightConfig {

    public enum IconPosition { HOTBAR, CROSSHAIR, TOP_LEFT_CORNER, TOP_RIGHT_CORNER, BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER }

    @Entry public static IconPosition iconPosition = IconPosition.HOTBAR;
    @Entry public static boolean showStaminaInIcon = true;
    @Entry public static boolean showStaminaInHungerBar = true;

}
