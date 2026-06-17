package com.diplomacia.bot;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class LogStore {
    private static final String KEY_LOGS = "logs";
    private static final int MAX_CHARS = 12000;

    static void append(Context context, String message) {
        SharedPreferences prefs = BotConfig.prefs(context);
        String line = "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] " + message + "\n";
        String logs = prefs.getString(KEY_LOGS, "") + line;
        if (logs.length() > MAX_CHARS) {
            logs = logs.substring(logs.length() - MAX_CHARS);
        }
        prefs.edit().putString(KEY_LOGS, logs).apply();
    }

    static String read(Context context) {
        return BotConfig.prefs(context).getString(KEY_LOGS, "Log yok.");
    }

    static void clear(Context context) {
        BotConfig.prefs(context).edit().remove(KEY_LOGS).apply();
    }
}
