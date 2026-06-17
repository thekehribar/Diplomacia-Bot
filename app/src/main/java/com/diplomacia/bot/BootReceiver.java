package com.diplomacia.bot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            BotConfig config = BotConfig.load(context, i);
            if (config.isReady()) {
                LogStore.append(context, "Hesap " + (i + 1) + " cihaz acilisinda tekrar zamanlandi.");
                BotWorker.schedule(context, i, TimeUnit.MINUTES.toMillis(1));
            }
        }
    }
}
