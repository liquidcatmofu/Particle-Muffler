package dev.liquidcatmofu.particlemuffler.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;

public final class ParticleInspection {
    private static final List<String> ORDERS = List.of("recent", "count", "name");
    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_SECONDS = 5;
    private static final int DEFAULT_RESULTS = 32;
    private static final Order DEFAULT_ORDER = Order.COUNT;
    private static final int MIN_SECONDS = 1;
    private static final int MAX_SECONDS = 30;
    private static final int MIN_RESULTS = 1;
    private static final int MAX_RESULTS = 256;

    private static Session activeSession;
    private static long sequence;

    private ParticleInspection() {
    }

    public static void registerCommands() {
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> register(dispatcher));
    }

    public static boolean isActive() {
        return activeSession != null;
    }

    public static void record(ResourceLocation particleId) {
        Session session = activeSession;
        if (session == null) {
            return;
        }

        session.record(particleId);
    }

    public static void tick(Minecraft client) {
        Session session = activeSession;
        if (session == null) {
            return;
        }

        session.remainingTicks--;
        if (session.remainingTicks > 0) {
            return;
        }

        activeSession = null;
        if (client.player != null) {
            session.sendResults(client);
        }
    }

    public static void clear() {
        activeSession = null;
    }

    private static void register(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> dispatcher) {
        dispatcher.register(ClientCommandRegistrationEvent.literal("particlemuffler")
                .then(ClientCommandRegistrationEvent.literal("inspect")
                        .executes(context -> start(context.getSource(), DEFAULT_SECONDS, DEFAULT_RESULTS, DEFAULT_ORDER))
                        .then(ClientCommandRegistrationEvent.argument("time", IntegerArgumentType.integer(MIN_SECONDS, MAX_SECONDS))
                                .executes(context -> start(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "time"),
                                        DEFAULT_RESULTS,
                                        DEFAULT_ORDER))
                                .then(ClientCommandRegistrationEvent.argument("max-number", IntegerArgumentType.integer(MIN_RESULTS, MAX_RESULTS))
                                        .executes(context -> start(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "time"),
                                                IntegerArgumentType.getInteger(context, "max-number"),
                                                DEFAULT_ORDER))
                                        .then(ClientCommandRegistrationEvent.argument("order", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ORDERS, builder))
                                                .executes(context -> start(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "time"),
                                                        IntegerArgumentType.getInteger(context, "max-number"),
                                                        StringArgumentType.getString(context, "order"))))))));
    }

    private static int start(ClientCommandRegistrationEvent.ClientCommandSourceStack source, int seconds, int maxResults, Order order) {
        activeSession = new Session(seconds * TICKS_PER_SECOND, maxResults, order);
        source.arch$sendSuccess(() -> Component.literal("Inspecting visible particles for " + seconds + "s...")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int start(ClientCommandRegistrationEvent.ClientCommandSourceStack source, int seconds, int maxResults, String orderName) {
        Order order = Order.fromName(orderName);
        if (order == null) {
            source.arch$sendFailure(Component.literal("Unknown order: " + orderName + ". Use recent, count, or name."));
            return 0;
        }

        return start(source, seconds, maxResults, order);
    }

    private enum Order {
        RECENT,
        COUNT,
        NAME;

        private static Order fromName(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "recent" -> RECENT;
                case "count" -> COUNT;
                case "name" -> NAME;
                default -> null;
            };
        }
    }

    private static final class Session {
        private final int maxResults;
        private final Order order;
        private final LinkedHashMap<ResourceLocation, Entry> entries = new LinkedHashMap<>();
        private boolean capped;
        private int remainingTicks;

        private Session(int remainingTicks, int maxResults, Order order) {
            this.remainingTicks = remainingTicks;
            this.maxResults = maxResults;
            this.order = order;
        }

        private void record(ResourceLocation particleId) {
            Entry entry = entries.get(particleId);
            if (entry != null) {
                entry.count++;
                entry.lastSeen = ++sequence;
                return;
            }

            if (entries.size() >= maxResults) {
                capped = true;
                return;
            }

            entries.put(particleId, new Entry(particleId, ++sequence));
        }

        private void sendResults(Minecraft client) {
            if (entries.isEmpty()) {
                client.gui.getChat().addMessage(Component.literal("No particles were seen during inspection.")
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }

            List<Entry> results = new ArrayList<>(entries.values());
            results.sort(comparator());

            client.gui.getChat().addMessage(Component.literal("Visible particles (" + results.size() + "):")
                    .withStyle(ChatFormatting.GREEN));
            for (Entry entry : results) {
                String particleId = entry.id.toString();
                Component idComponent = Component.literal(particleId)
                        .withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, particleId))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy " + particleId))));
                client.gui.getChat().addMessage(Component.literal("- ")
                        .append(idComponent)
                        .append(Component.literal(" x" + entry.count).withStyle(ChatFormatting.GRAY)));
            }

            if (capped) {
                client.gui.getChat().addMessage(Component.literal("Result limit reached; increase max-number to inspect more particles.")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        private Comparator<Entry> comparator() {
            return switch (order) {
                case RECENT -> Comparator.comparingLong((Entry entry) -> entry.lastSeen).reversed();
                case COUNT -> Comparator.comparingInt((Entry entry) -> entry.count).reversed()
                        .thenComparing(entry -> entry.id.toString());
                case NAME -> Comparator.comparing(entry -> entry.id.toString());
            };
        }
    }

    private static final class Entry {
        private final ResourceLocation id;
        private int count = 1;
        private long lastSeen;

        private Entry(ResourceLocation id, long lastSeen) {
            this.id = id;
            this.lastSeen = lastSeen;
        }
    }
}
