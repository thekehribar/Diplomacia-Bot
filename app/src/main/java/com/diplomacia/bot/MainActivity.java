package com.diplomacia.bot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(13, 22, 40);
    private static final int FIELD = Color.rgb(9, 14, 27);
    private static final int CARD = Color.rgb(28, 40, 61);
    private static final int TEXT = Color.rgb(242, 246, 255);
    private static final int MUTED = Color.rgb(168, 174, 190);
    private static final int ACCENT = Color.rgb(31, 190, 174);
    private static final int GREEN = Color.rgb(47, 204, 113);
    private static final int BLUE = Color.rgb(22, 72, 124);
    private static final int DISABLED = Color.rgb(50, 56, 78);

    private int activeAccount = 0;
    private String selectedSkill = BotConfig.SKILLS[2];
    private String selectedType = BotConfig.TYPES[0];
    private Button account1Button;
    private Button account2Button;
    private Button moneyButton;
    private Button diamondButton;
    private Button kislaButton;
    private Button savasButton;
    private Button bilimButton;
    private EditText tokenInput;
    private EditText donationAmountInput;
    private Spinner queueSkillSpinner;
    private EditText queueCountInput;
    private TextView statusText;
    private TextView queueText;
    private TextView logsText;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private final Runnable statusTicker = new Runnable() {
        @Override
        public void run() {
            triggerDueRuns();
            refreshStatus();
            statusHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadConfig();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenInput != null) {
            loadConfig();
            refreshStatus();
        }
        statusHandler.removeCallbacks(statusTicker);
        statusHandler.post(statusTicker);
    }

    @Override
    protected void onPause() {
        statusHandler.removeCallbacks(statusTicker);
        super.onPause();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        scrollView.addView(root);

        TextView title = label("DIPLOMACIA BOT", 24, ACCENT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView author = label("Hazirlayan: @thekehribar", 13, MUTED, true);
        author.setGravity(Gravity.CENTER);
        root.addView(author);

        LinearLayout accounts = row();
        account1Button = tabButton("HESAP 1");
        account2Button = tabButton("HESAP 2");
        accounts.addView(account1Button, weightParams());
        accounts.addView(account2Button, weightParams());
        root.addView(accounts);

        statusText = cardText("Durum yukleniyor...");
        root.addView(statusText);

        LinearLayout tokenRow = row();
        tokenInput = input("Bearer Token");
        tokenInput.setMinLines(2);
        tokenInput.setTextColor(TEXT);
        tokenInput.setHintTextColor(MUTED);
        tokenInput.setBackground(card(FIELD, FIELD));
        tokenRow.addView(tokenInput, new LinearLayout.LayoutParams(0, dp(74), 1f));
        Button save = actionButton("Kaydet", BLUE);
        tokenRow.addView(save, new LinearLayout.LayoutParams(dp(116), dp(74)));
        root.addView(tokenRow);

        Button connect = actionButton("BAGLAN", ACCENT);
        root.addView(connect, fullButtonParams());
        Button backgroundPermission = actionButton("Arka plan izni ver", BLUE);
        root.addView(backgroundPermission, fullButtonParams());

        root.addView(sectionTitle("ODEME"));
        LinearLayout paymentRow = row();
        moneyButton = choiceButton("Para");
        diamondButton = choiceButton("Elmas");
        paymentRow.addView(moneyButton, weightParams());
        paymentRow.addView(diamondButton, weightParams());
        root.addView(paymentRow);

        root.addView(sectionTitle("BAGIS"));
        LinearLayout donationRow = row();
        donationAmountInput = input("Bagis miktari");
        donationAmountInput.setGravity(Gravity.CENTER);
        donationAmountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        donationRow.addView(donationAmountInput, new LinearLayout.LayoutParams(0, dp(62), 1f));
        Button donate = actionButton("Bagis yap", Color.rgb(245, 178, 50));
        donationRow.addView(donate, new LinearLayout.LayoutParams(dp(128), dp(62)));
        root.addView(donationRow);

        root.addView(sectionTitle("BECERILER (kuyruk bosken doner)"));
        LinearLayout skillRow = row();
        kislaButton = choiceButton("Kisla");
        savasButton = choiceButton("Savas");
        bilimButton = choiceButton("Bilim");
        skillRow.addView(kislaButton, weightParams());
        skillRow.addView(savasButton, weightParams());
        skillRow.addView(bilimButton, weightParams());
        root.addView(skillRow);

        root.addView(sectionTitle("EMIR KUYRUGU"));
        LinearLayout queueRow = row();
        queueSkillSpinner = spinner(new String[]{"kisla", "savas_teknikleri", "bilim_insani"});
        queueRow.addView(queueSkillSpinner, new LinearLayout.LayoutParams(0, dp(62), 1f));
        queueCountInput = input("Adet");
        queueCountInput.setText("1");
        queueCountInput.setGravity(Gravity.CENTER);
        queueCountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        queueRow.addView(queueCountInput, new LinearLayout.LayoutParams(dp(86), dp(62)));
        Button addQueue = actionButton("Ekle", GREEN);
        queueRow.addView(addQueue, new LinearLayout.LayoutParams(dp(96), dp(62)));
        root.addView(queueRow);

        queueText = label("Kuyruk bos. Sirayla yukseltmek icin emir ekle.", 14, MUTED, false);
        root.addView(queueText);
        Button removeQueue = actionButton("Ilk kuyruk emrini sil", BLUE);
        root.addView(removeQueue, fullButtonParams());

        LinearLayout runButtons = row();
        Button start = actionButton("BASLAT", GREEN);
        Button stop = actionButton("DURDUR", DISABLED);
        runButtons.addView(start, weightParams());
        runButtons.addView(stop, weightParams());
        root.addView(runButtons);

        TextView logTitle = label("LOG", 15, Color.rgb(245, 178, 50), true);
        root.addView(logTitle);
        logsText = cardText("Log yok.");
        logsText.setMinLines(6);
        logsText.setTextIsSelectable(true);
        root.addView(logsText);
        Button clearLogs = actionButton("Loglari temizle", DISABLED);
        root.addView(clearLogs, fullButtonParams());

        account1Button.setOnClickListener(v -> switchAccount(0));
        account2Button.setOnClickListener(v -> switchAccount(1));
        moneyButton.setOnClickListener(v -> selectType(BotConfig.TYPES[0]));
        diamondButton.setOnClickListener(v -> selectType(BotConfig.TYPES[1]));
        kislaButton.setOnClickListener(v -> selectSkill("kisla"));
        savasButton.setOnClickListener(v -> selectSkill("savas_teknikleri"));
        bilimButton.setOnClickListener(v -> selectSkill("bilim_insani"));
        connect.setOnClickListener(v -> startWebViewLogin());
        backgroundPermission.setOnClickListener(v -> requestBackgroundPermission());
        save.setOnClickListener(v -> {
            saveConfig(BotConfig.load(this, activeAccount).enabled);
            LogStore.append(this, accountName(activeAccount) + " ayarlari kaydedildi.");
            refreshStatus();
        });
        donate.setOnClickListener(v -> donate());
        addQueue.setOnClickListener(v -> addQueueEntry());
        removeQueue.setOnClickListener(v -> removeFirstQueueEntry());
        start.setOnClickListener(v -> {
            saveConfig(true);
            BotWorker.start(this, activeAccount);
            refreshStatus();
        });
        stop.setOnClickListener(v -> {
            BotWorker.stop(this, activeAccount);
            LogStore.append(this, accountName(activeAccount) + " bot durduruldu.");
            refreshStatus();
        });
        clearLogs.setOnClickListener(v -> {
            LogStore.clear(this);
            refreshStatus();
        });

        setContentView(scrollView);
    }

    private void startWebViewLogin() {
        Intent intent = new Intent(this, LoginWebViewActivity.class);
        intent.putExtra("accountIndex", activeAccount);
        startActivity(intent);
    }

    private void requestBackgroundPermission() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                showMessage("Arka plan izni", "Pil optimizasyonu zaten kapali gorunuyor.");
                return;
            }
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception error) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            showMessage("Arka plan izni", "Acilan ekrandan pil optimizasyonunu kapat.");
        }
    }

    private void selectType(String value) {
        selectedType = value;
        updateChoiceButtons();
    }

    private void selectSkill(String value) {
        selectedSkill = value;
        updateChoiceButtons();
    }

    private void updateChoiceButtons() {
        styleChoice(moneyButton, BotConfig.TYPES[0].equals(selectedType));
        styleChoice(diamondButton, BotConfig.TYPES[1].equals(selectedType));
        styleChoice(kislaButton, "kisla".equals(selectedSkill));
        styleChoice(savasButton, "savas_teknikleri".equals(selectedSkill));
        styleChoice(bilimButton, "bilim_insani".equals(selectedSkill));
        styleTab(account1Button, activeAccount == 0);
        styleTab(account2Button, activeAccount == 1);
    }

    private void switchAccount(int accountIndex) {
        saveConfig(BotConfig.load(this, activeAccount).enabled);
        activeAccount = accountIndex;
        loadConfig();
        refreshStatus();
    }

    private void loadConfig() {
        BotConfig config = BotConfig.load(this, activeAccount);
        tokenInput.setText(config.token);
        selectedSkill = config.skill;
        selectedType = config.type;
        int queueSkillIndex = BotConfig.asList(new String[]{"kisla", "savas_teknikleri", "bilim_insani"}).indexOf(config.skill);
        queueSkillSpinner.setSelection(Math.max(queueSkillIndex, 0));
        updateChoiceButtons();
    }

    private void saveConfig(boolean enabled) {
        new BotConfig(
                activeAccount,
                tokenInput.getText().toString().trim(),
                selectedSkill,
                selectedType,
                enabled,
                BotConfig.load(this, activeAccount).queue
        ).save(this);
    }

    private void addQueueEntry() {
        int count;
        try {
            count = Integer.parseInt(queueCountInput.getText().toString().trim());
        } catch (NumberFormatException error) {
            count = 1;
        }
        BotConfig config = BotConfig.load(this, activeAccount);
        List<BotConfig.QueueEntry> queue = new ArrayList<>(config.queue);
        queue.add(new BotConfig.QueueEntry(String.valueOf(queueSkillSpinner.getSelectedItem()), selectedType, count));
        config.withQueue(queue).save(this);
        LogStore.append(this, accountName(activeAccount) + " kuyruga eklendi.");
        refreshStatus();
    }

    private void removeFirstQueueEntry() {
        BotConfig config = BotConfig.load(this, activeAccount);
        List<BotConfig.QueueEntry> queue = new ArrayList<>(config.queue);
        if (!queue.isEmpty()) {
            queue.remove(0);
            config.withQueue(queue).save(this);
            LogStore.append(this, accountName(activeAccount) + " ilk kuyruk emri silindi.");
        }
        refreshStatus();
    }

    private void donate() {
        long amount;
        try {
            amount = Long.parseLong(donationAmountInput.getText().toString().trim());
        } catch (NumberFormatException error) {
            showMessage("Bagis hatasi", "Gecerli bir miktar gir.");
            return;
        }
        if (amount <= 0L) {
            showMessage("Bagis hatasi", "Miktar 0'dan buyuk olmali.");
            return;
        }

        saveConfig(BotConfig.load(this, activeAccount).enabled);
        BotConfig config = BotConfig.load(this, activeAccount);
        if (config.token.trim().isEmpty()) {
            showMessage("Bagis hatasi", "Once token kaydet.");
            return;
        }

        LogStore.append(this, accountName(activeAccount) + " bagis gonderiliyor: " + amount);
        refreshStatus();
        new Thread(() -> {
            try {
                DiplomaciaApi.ApiResult result = DiplomaciaApi.donate(config, amount);
                runOnUiThread(() -> handleDonationResult(result));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    LogStore.append(this, accountName(activeAccount) + " bagis hatasi: " + error.getMessage());
                    refreshStatus();
                    showMessage("Bagis hatasi", error.getMessage());
                });
            }
        }).start();
    }

    private void handleDonationResult(DiplomaciaApi.ApiResult result) {
        if (result.isSuccessful() && result.json.optBoolean("success", false)) {
            String recipient = result.json.optString("recipient_username", "alici");
            long amount = result.json.optLong("amount", 0L);
            long newBalance = result.json.optLong("new_balance", -1L);
            String message = "Bagis basarili: " + amount + " -> " + recipient;
            if (newBalance >= 0L) {
                message += " | Yeni bakiye: " + newBalance;
            }
            LogStore.append(this, accountName(activeAccount) + " " + message);
            refreshStatus();
            showMessage("Bagis basarili", message);
            return;
        }

        String error = result.json.optString("error", result.raw.isEmpty() ? "HTTP " + result.status : result.raw);
        LogStore.append(this, accountName(activeAccount) + " bagis basarisiz: " + error);
        refreshStatus();
        showMessage("Bagis hatasi", error);
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message == null || message.trim().isEmpty() ? "Bilinmeyen hata" : message)
                .setPositiveButton("Tamam", null)
                .show();
    }

    private void refreshStatus() {
        BotConfig config = BotConfig.load(this, activeAccount);
        long nextRunAt = BotConfig.prefs(this).getLong(BotWorker.nextRunKey(activeAccount), 0L);
        long remainingMs = nextRunAt - System.currentTimeMillis();
        String next = BotWorker.isRunning(activeAccount)
                ? "istek atiliyor"
                : remainingMs > 0
                ? new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(nextRunAt)) + " | Kalan: " + formatDuration(remainingMs)
                : "hazir";
        statusText.setText(accountName(activeAccount) + " | " + (config.enabled ? "calisiyor" : "durdu") + " | Sonraki: " + next);
        queueText.setText(queueSummary(config.queue));
        logsText.setText(LogStore.read(this));
    }

    private void triggerDueRuns() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 2; i++) {
            BotConfig config = BotConfig.load(this, i);
            long nextRunAt = BotConfig.prefs(this).getLong(BotWorker.nextRunKey(i), 0L);
            if (config.isReady() && nextRunAt > 0L && nextRunAt <= now && !BotWorker.isRunning(i)) {
                BotWorker.runNow(this, i);
            }
        }
    }

    private String queueSummary(List<BotConfig.QueueEntry> queue) {
        if (queue.isEmpty()) {
            return "Kuyruk bos. Sirayla yukseltmek icin emir ekle.";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < queue.size(); i++) {
            builder.append(i + 1).append(". ").append(queue.get(i)).append('\n');
        }
        return builder.toString();
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private String accountName(int accountIndex) {
        return "Hesap " + (accountIndex + 1);
    }

    private TextView sectionTitle(String text) {
        TextView view = label("| " + text, 16, TEXT, true);
        view.setPadding(0, dp(18), 0, dp(8));
        return view;
    }

    private TextView cardText(String text) {
        TextView view = label(text, 15, MUTED, true);
        view.setBackground(card(FIELD, Color.rgb(72, 83, 106)));
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        return view;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }

    private EditText input(String hint) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setSingleLine(false);
        view.setTextColor(TEXT);
        view.setHintTextColor(MUTED);
        view.setBackground(card(FIELD, FIELD));
        view.setPadding(dp(12), 0, dp(12), 0);
        return view;
    }

    private Spinner spinner(String[] values) {
        Spinner view = new Spinner(this);
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        view.setBackground(card(FIELD, FIELD));
        return view;
    }

    private Button tabButton(String text) {
        Button button = actionButton(text, DISABLED);
        styleTab(button, false);
        return button;
    }

    private Button choiceButton(String text) {
        Button button = actionButton(text, FIELD);
        styleChoice(button, false);
        return button;
    }

    private Button actionButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(card(color, color));
        return button;
    }

    private void styleTab(Button button, boolean selected) {
        button.setTextColor(selected ? ACCENT : MUTED);
        button.setBackground(card(BG, selected ? ACCENT : BG));
    }

    private void styleChoice(Button button, boolean selected) {
        button.setTextColor(selected ? TEXT : MUTED);
        button.setBackground(card(selected ? CARD : FIELD, selected ? ACCENT : Color.rgb(50, 65, 92)));
    }

    private GradientDrawable card(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        return row;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60));
        params.setMargins(0, dp(8), 0, dp(8));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

}
