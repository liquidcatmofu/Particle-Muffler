package dev.liquidcatmofu.particlemuffler;

import dev.liquidcatmofu.particlemuffler.registry.ModBlockEntities;
import dev.liquidcatmofu.particlemuffler.registry.ModBlocks;
import dev.liquidcatmofu.particlemuffler.registry.ModItems;
import dev.liquidcatmofu.particlemuffler.config.ParticleMufflerConfig;

public final class Particlemuffler {
    public static final String MOD_ID = "particlemuffler";

    private Particlemuffler() {
    }

    public static void init() {
        ParticleMufflerConfig.load();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
    }
}
