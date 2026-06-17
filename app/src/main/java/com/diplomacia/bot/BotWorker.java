package com.diplomacia.bot;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class BotWorker extends Worker {
    private static final String WORK_NAME_PREFIX = "diplomacia_bot_work_account_";
    private static final Object RUN_LOCK = new Object();
    private static final boolean[] RUNNING = new boolean[2];

    public BotWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        int accountIndex = getInputData().getInt("accountIndex", 0);

        if (!markRunning(accountIndex)) {
            return Result.success();
        }
        try {
            runAccount(context, accountIndex);
        } finally {
            clearRunning(accountIndex);
        }
        return Result.success();
    }

    private static void runAccount(Context context, int accountIndex) {
        BotConfig config = BotConfig.load(context, accountIndex);
        if (!config.isReady()) {
            LogStore.append(context, accountName(accountIndex) + " bot durdu veya token bos.");
            return;
        }

        try {
            Command command = nextCommand(config);
            BotConfig runConfig = config.withCommand(command.skill, command.type);
            LogStore.append(context, accountName(accountIndex) + " istek atiliyor: " + runConfig.skill + " (" + runConfig.type + ")");
            DiplomaciaApi.ApiResult result = DiplomaciaApi.upgrade(runConfig);
            long waitMs = computeWaitMs(result.json, runConfig.bufferMs);

            if (result.isSuccessful()) {
                LogStore.append(context, accountName(accountIndex) + " OK: " + summarizeSuccess(result.json));
                if (command.queued) {
                    consumeQueue(context, accountIndex, config);
                }
            } else if (result.status == 429) {
                LogStore.append(context, accountName(accountIndex) + " bekleme gerekli: " + result.raw);
            } else if (result.status == 401) {
                stop(context, accountIndex);
                LogStore.append(context, accountName(accountIndex) + " token gecersiz. Bot durduruldu.");
                return;
            } else {
                LogStore.append(context, accountName(accountIndex) + " HTTP " + result.status + ": " + result.raw);
            }

            if (BotConfig.load(context, accountIndex).enabled) {
                schedule(context, accountIndex, Math.max(waitMs, 60000L));
            }
        } catch (Exception error) {
            LogStore.append(context, accountName(accountIndex) + " hata: " + error.getMessage());
            if (BotConfig.load(context, accountIndex).enabled) {
                schedule(context, accountIndex, TimeUnit.MINUTES.toMillis(5));
            }
        }
    }

    static void start(Context context, int accountIndex) {
        BotConfig config = BotConfig.load(context, accountIndex).withEnabled(true);
        config.save(context);
        LogStore.append(context, accountName(accountIndex) + " bot baslatildi.");
        runNow(context, accountIndex);
    }

    static void runNow(Context context, int accountIndex) {
        Context appContext = context.getApplicationContext();
        WorkManager.getInstance(appContext).cancelUniqueWork(workName(accountIndex));
        BotConfig.prefs(appContext).edit().putLong(nextRunKey(accountIndex), System.currentTimeMillis()).apply();
        if (!markRunning(accountIndex)) {
            return;
        }
        new Thread(() -> {
            try {
                runAccount(appContext, accountIndex);
            } finally {
                clearRunning(accountIndex);
            }
        }).start();
    }

    static void stop(Context context, int accountIndex) {
        BotConfig.load(context, accountIndex).withEnabled(false).save(context);
        BotConfig.prefs(context).edit().putLong(nextRunKey(accountIndex), 0L).apply();
        WorkManager.getInstance(context).cancelUniqueWork(workName(accountIndex));
    }

    static void schedule(Context context, int accountIndex, long delayMs) {
        BotConfig.prefs(context).edit().putLong(nextRunKey(accountIndex), System.currentTimeMillis() + delayMs).apply();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data data = new Data.Builder().putInt("accountIndex", accountIndex).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BotWorker.class)
                .setConstraints(constraints)
                .setInputData(data)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(workName(accountIndex), ExistingWorkPolicy.REPLACE, request);
    }

    static String nextRunKey(int accountIndex) {
        return BotConfig.prefix(accountIndex) + "nextRunAt";
    }

    static boolean isRunning(int accountIndex) {
        synchronized (RUN_LOCK) {
            return isValidAccount(accountIndex) && RUNNING[accountIndex];
        }
    }

    private static boolean markRunning(int accountIndex) {
        synchronized (RUN_LOCK) {
            if (!isValidAccount(accountIndex) || RUNNING[accountIndex]) {
                return false;
            }
            RUNNING[accountIndex] = true;
            return true;
        }
    }

    private static void clearRunning(int accountIndex) {
        synchronized (RUN_LOCK) {
            if (isValidAccount(accountIndex)) {
                RUNNING[accountIndex] = false;
            }
        }
    }

    private static boolean isValidAccount(int accountIndex) {
        return accountIndex >= 0 && accountIndex < RUNNING.length;
    }

    private static String workName(int accountIndex) {
        return WORK_NAME_PREFIX + accountIndex;
    }

    private static String accountName(int accountIndex) {
        return "Hesap " + (accountIndex + 1);
    }

    private static Command nextCommand(BotConfig config) {
        if (!config.queue.isEmpty()) {
            BotConfig.QueueEntry entry = config.queue.get(0);
            return new Command(entry.skill, entry.type, true);
        }
        return new Command(config.skill, config.type, false);
    }

    private static void consumeQueue(Context context, int accountIndex, BotConfig config) {
        List<BotConfig.QueueEntry> queue = new ArrayList<>(config.queue);
        if (queue.isEmpty()) {
            return;
        }
        BotConfig.QueueEntry current = queue.get(0);
        if (current.count <= 1) {
            queue.remove(0);
            LogStore.append(context, accountName(accountIndex) + " kuyruk emri tamamlandi: " + current.skill + " (" + current.type + ")");
        } else {
            queue.set(0, new BotConfig.QueueEntry(current.skill, current.type, current.count - 1));
        }
        config.withQueue(queue).save(context);
    }

    private static long computeWaitMs(JSONObject json, long bufferMs) {
        long cooldown = json.optLong("cooldown_ms", 0L);
        long remaining = json.optLong("remaining_ms", 0L);
        long pending = 0L;
        String pendingAt = json.optString("pending_at", "");
        if (!pendingAt.isEmpty()) {
            try {
                pending = parseIsoTime(pendingAt) - new Date().getTime();
            } catch (Exception ignored) {
                pending = 0L;
            }
        }
        long base = Math.max(Math.max(cooldown, remaining), pending);
        return Math.max(base, 0L) + bufferMs;
    }

    private static String summarizeSuccess(JSONObject json) {
        String skill = json.optString("skill", "skill");
        int current = json.optInt("current_level", -1);
        int target = json.optInt("target_level", -1);
        long cooldown = json.optLong("cooldown_ms", 0L);
        if (current >= 0 && target >= 0) {
            return skill + " " + current + " -> " + target + ", cooldown=" + cooldown + "ms";
        }
        return json.toString();
    }

    private static long parseIsoTime(String value) throws Exception {
        String normalized = value.endsWith("Z") ? value.substring(0, value.length() - 1) + "+0000" : value;
        normalized = normalized.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
        normalized = normalized.replaceFirst("\\.(\\d{3})\\d+", ".$1");
        if (!normalized.matches(".*[+-]\\d{4}$")) {
            normalized = normalized + "+0000";
        }
        SimpleDateFormat format = normalized.contains(".")
                ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                : new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.parse(normalized).getTime();
    }

    private static final class Command {
        final String skill;
        final String type;
        final boolean queued;

        Command(String skill, String type, boolean queued) {
            this.skill = skill;
            this.type = type;
            this.queued = queued;
        }
    }
}
