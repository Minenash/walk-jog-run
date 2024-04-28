package com.minenash.walk_jog_run.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minenash.walk_jog_run.WalkJogRun;
import net.fabricmc.loader.api.FabricLoader;

import java.beans.Transient;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfig {

    public static double STROLLING_SPEED_MODIFIER = -0.3;
    public static double SPRINTING_SPEED_MODIFIER = 0.3;
    public static double BASE_WALKING_SPEED_MODIFIER = 0.0;

    public static boolean STAMINA_ENABLED = true;
    public static int STAMINA_PER_FOOD_LEVEL = 40;
    public static int STAMINA_DEPLETION_PER_TICK = 2;
    public static int STAMINA_RECOVERY_WALKING = 1;
    public static int STAMINA_RECOVERY_STROLLING = 2;

    public static int STAMINA_EXHAUSTED_SLOWNESS_DURATION_IN_TICKS = 100;
    public static boolean STAMINA_EXHAUSTED_SLOWNESS_SHOW_PARTICLES = true;



    private static final Path path = FabricLoader.getInstance().getConfigDir().resolve("walk-jog-run.json");
    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .setPrettyPrinting()
            .create();

    public static void read() {

        try {
            WalkJogRun.SERVER_CONFIG_JSON = Files.readString(path);
            gson.fromJson(WalkJogRun.SERVER_CONFIG_JSON, ServerConfig.class);
        }
        catch (Exception e) {
            write();
        }

    }

    public static void applyConfig(String json) {

        try {
            WalkJogRun.SERVER_CONFIG_JSON = json;
            gson.fromJson(json, ServerConfig.class);
        }
        catch (Exception e) {
            write();
        }

    }

    public static void write() {
        try {
            if (!Files.exists(path)) Files.createFile(path);
            Files.write(path, gson.toJson(ServerConfig.class.newInstance()).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
