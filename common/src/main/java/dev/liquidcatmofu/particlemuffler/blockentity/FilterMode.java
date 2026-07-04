package dev.liquidcatmofu.particlemuffler.blockentity;

public enum FilterMode {
    BLACKLIST,
    WHITELIST;

    public static FilterMode byName(String name) {
        for (FilterMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }

        return BLACKLIST;
    }
}
