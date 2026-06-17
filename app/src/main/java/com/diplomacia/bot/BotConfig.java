package com.diplomacia.bot;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

final class BotConfig {
    static final String[] SKILLS = {"savas_teknikleri", "kisla", "bilim_insani"};
    static final String[] TYPES = {"money", "diamond"};
    static final String DEFAULT_BASE_URL = "https://diplomacia.com.tr";
    static final long DEFAULT_BUFFER_MS = 3000L;

    final int accountIndex;
    final String token;
    final String baseUrl;
    final String skill;
    final String type;
    final long bufferMs;
    final boolean enabled;
    final List<QueueEntry> queue;

    BotConfig(int accountIndex, String token, String skill, String type, boolean enabled, List<QueueEntry> queue) {
        this.accountIndex = accountIndex;
        this.token = token == null ? "" : token.trim();
        this.baseUrl = DEFAULT_BASE_URL;
        this.skill = skill == null || skill.trim().isEmpty() ? SKILLS[2] : skill.trim();
        this.type = type == null || type.trim().isEmpty() ? TYPES[0] : type.trim();
        this.bufferMs = DEFAULT_BUFFER_MS;
        this.enabled = enabled;
        this.queue = queue == null ? new ArrayList<>() : new ArrayList<>(queue);
    }

    static BotConfig load(Context context, int accountIndex) {
        SharedPreferences prefs = prefs(context);
        String prefix = prefix(accountIndex);
        boolean legacy = accountIndex == 0 && !prefs.contains(prefix + "token");
        return new BotConfig(
                accountIndex,
                prefs.getString(legacy ? "token" : prefix + "token", ""),
                prefs.getString(legacy ? "skill" : prefix + "skill", SKILLS[2]),
                prefs.getString(legacy ? "type" : prefix + "type", TYPES[0]),
                prefs.getBoolean(legacy ? "enabled" : prefix + "enabled", false),
                parseQueue(prefs.getString(legacy ? "queue" : prefix + "queue", ""))
        );
    }

    void save(Context context) {
        String prefix = prefix(accountIndex);
        prefs(context).edit()
                .putString(prefix + "token", token)
                .putString(prefix + "skill", skill)
                .putString(prefix + "type", type)
                .putBoolean(prefix + "enabled", enabled)
                .putString(prefix + "queue", formatQueue(queue))
                .apply();
    }

    BotConfig withEnabled(boolean value) {
        return new BotConfig(accountIndex, token, skill, type, value, queue);
    }

    BotConfig withCommand(String commandSkill, String commandType) {
        return new BotConfig(accountIndex, token, commandSkill, commandType, enabled, queue);
    }

    BotConfig withQueue(List<QueueEntry> newQueue) {
        return new BotConfig(accountIndex, token, skill, type, enabled, newQueue);
    }

    boolean isReady() {
        return enabled && token != null && !token.trim().isEmpty();
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("diplomacia_bot", Context.MODE_PRIVATE);
    }

    static String prefix(int accountIndex) {
        return "account" + (accountIndex + 1) + ".";
    }

    static List<String> asList(String[] values) {
        List<String> list = new ArrayList<>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }

    static List<QueueEntry> parseQueue(String value) {
        List<QueueEntry> entries = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return entries;
        }
        for (String part : value.split(";")) {
            String[] pieces = part.split(",", -1);
            if (pieces.length != 3) {
                continue;
            }
            try {
                int count = Integer.parseInt(pieces[2]);
                if (count > 0) {
                    entries.add(new QueueEntry(pieces[0], pieces[1], count));
                }
            } catch (NumberFormatException ignored) {
                // Skip broken queue rows.
            }
        }
        return entries;
    }

    static String formatQueue(List<QueueEntry> queue) {
        StringBuilder builder = new StringBuilder();
        for (QueueEntry entry : queue) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(entry.skill).append(',').append(entry.type).append(',').append(entry.count);
        }
        return builder.toString();
    }

    static final class QueueEntry {
        final String skill;
        final String type;
        final int count;

        QueueEntry(String skill, String type, int count) {
            this.skill = skill == null || skill.trim().isEmpty() ? SKILLS[2] : skill.trim();
            this.type = type == null || type.trim().isEmpty() ? TYPES[0] : type.trim();
            this.count = Math.max(1, count);
        }

        @Override
        public String toString() {
            return count + "x " + skill + " (" + type + ")";
        }
    }

}
