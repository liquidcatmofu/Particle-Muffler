package dev.liquidcatmofu.particlemuffler.config;

import dev.architectury.platform.Platform;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ParticleMufflerConfig {
    private static final String FILE_NAME = "particlemuffler.toml";
    private static final int DEFAULT_PRUNING_INTERVAL_TICKS = 20;
    private static final int MIN_PRUNING_INTERVAL_TICKS = 1;
    private static final int MAX_PRUNING_INTERVAL_TICKS = 20 * 60;

    private static int pruningIntervalTicks = DEFAULT_PRUNING_INTERVAL_TICKS;

    private ParticleMufflerConfig() {
    }

    public static void load() {
        Path path = Platform.getConfigFolder().resolve(FILE_NAME);
        try {
            if (Files.notExists(path)) {
                writeDefault(path);
                return;
            }

            read(path);
        } catch (IOException exception) {
            pruningIntervalTicks = DEFAULT_PRUNING_INTERVAL_TICKS;
            System.err.println("[Particle Muffler] Failed to load config: " + exception.getMessage());
        }
    }

    public static int pruningIntervalTicks() {
        return pruningIntervalTicks;
    }

    private static void read(Path path) throws IOException {
        pruningIntervalTicks = DEFAULT_PRUNING_INTERVAL_TICKS;
        String section = "";
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (String line : lines) {
            String trimmed = stripComment(line).trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }

            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex < 0) {
                continue;
            }

            String key = trimmed.substring(0, equalsIndex).trim();
            String value = trimmed.substring(equalsIndex + 1).trim();
            if ("client".equals(section) && "pruningIntervalTicks".equals(key)) {
                pruningIntervalTicks = clamp(parseInt(value, DEFAULT_PRUNING_INTERVAL_TICKS));
            }
        }
    }

    private static void writeDefault(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, defaultToml(), StandardCharsets.UTF_8);
        pruningIntervalTicks = DEFAULT_PRUNING_INTERVAL_TICKS;
    }

    private static String defaultToml() {
        return """
                [client]
                # How often the client removes stale Particle Muffler entries from its local registry.
                # 20 ticks = 1 second. Minimum: 1. Maximum: 1200.
                pruningIntervalTicks = 20
                """;
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int clamp(int value) {
        return Math.max(MIN_PRUNING_INTERVAL_TICKS, Math.min(MAX_PRUNING_INTERVAL_TICKS, value));
    }
}
