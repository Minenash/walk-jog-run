package com.minenash.walk_jog_run.config;

public class ClientConfig extends MidnightConfig {

    public enum IconPosition { HOTBAR, CROSSHAIR, TOP_LEFT_CORNER, TOP_RIGHT_CORNER, BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER }
    public enum HungerBarColorState { STAMINA_LEFT, STAMINA_DEPLETED}

    @Entry public static IconPosition iconPosition = IconPosition.HOTBAR;
    @Entry public static boolean showStaminaInIcon = true;
    @Entry public static boolean showStaminaInHungerBar = true;

    @Entry(isColor = true)
    public static String hungerBarStaminaColor = "#4e4e4e";

    @Entry public static HungerBarColorState hungerBarColorState = HungerBarColorState.STAMINA_DEPLETED;



}
