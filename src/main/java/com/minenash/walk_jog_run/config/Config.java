package com.minenash.walk_jog_run.config;

public class Config {

    public static double STROLLING_SPEED_MODIFIER = -0.3;
    public static double SPRINTING_SPEED_MODIFIER = 0.3;
    public static double BASE_WALKING_SPEED = 0.7;

    public static int STAMINA_PER_FOOD_LEVEL = 40;
    public static int STAMINA_DEPLETION_PER_TICK = 2;
    public static int STAMINA_RECOVERY_WALKING = 1;
    public static int STAMINA_RECOVERY_STROLLING = 2;

    public static int STAMINA_EXHAUSTED_SLOWNESS_DURATION_IN_TICKS = 100;
    public static boolean STAMINA_EXHAUSTED_SLOWNESS_SHOW_PARTICLES = true;

}
