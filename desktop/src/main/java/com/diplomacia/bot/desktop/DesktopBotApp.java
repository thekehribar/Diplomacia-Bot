package com.diplomacia.bot.desktop;

import org.json.JSONObject;

import javax.swing.DefaultListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DesktopBotApp {
    private static final Color BACKGROUND = new Color(13, 22, 40);
    private static final Color CARD = new Color(28, 40, 61);
    private static final Color FIELD = new Color(9, 14, 27);
    private static final Color TEXT = new Color(242, 246, 255);
    private static final Color MUTED = new Color(168, 174, 190);
    private static final Color ACCENT = new Color(31, 190, 174);
    private static final Color GREEN = new Color(47, 204, 113);
    private static final Color BLUE = new Color(22, 72, 124);
    private static final Color DISABLED = new Color(50, 56, 78);

    private static final String[] SKILLS = {"savas_teknikleri", "kisla", "bilim_insani"};
    private static final String[] TYPES = {"money", "diamond"};
    private static final String DEFAULT_BASE_URL = "https://diplomacia.com.tr";
    private static final String DONATION_RECIPIENT_ID = "ec756c8c-d06a-474f-973e-6fdec9cb58c6";
    private static final long DEFAULT_BUFFER_MS = 3000L;
    private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+([A-Za-z0-9._\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");
    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"), ".diplomacia-bot-desktop.properties");

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "diplomacia-bot");
        thread.setDaemon(true);
        return thread;
    });

    private JFrame frame;
    private JButton account1Button;
    private JButton account2Button;
    private JTextArea tokenInput;
    private String selectedSkill;
    private String selectedType;
    private String selectedQueueSkill;
    private String selectedQueueType;
    private List<JToggleButton> skillButtons;
    private List<JToggleButton> typeButtons;
    private List<JToggleButton> queueSkillButtons;
    private List<JToggleButton> queueTypeButtons;
    private JSpinner donationAmountInput;
    private JSpinner queueCountInput;
    private DefaultListModel<QueueEntry> queueModel;
    private JList<QueueEntry> queueList;
    private JLabel statusLabel;
    private JTextArea logArea;
    private final ScheduledFuture<?>[] scheduledRuns = new ScheduledFuture<?>[2];
    private final Config[] accounts = new Config[2];
    private final long[] nextRunAt = new long[2];
    private int activeAccount;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopBotApp().show());
    }

    private void show() {
        configureTheme();
        accounts[0] = Config.load(0);
        accounts[1] = Config.load(1);
        activeAccount = 0;
        Config config = accounts[activeAccount];
        selectedSkill = config.skill;
        selectedType = config.type;
        selectedQueueSkill = config.skill;
        selectedQueueType = config.type;

        frame = new JFrame("Diplomacia Bot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(840, 720);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(12, 12));
        frame.getContentPane().setBackground(BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(18, 20, 8, 20));
        JLabel title = new JLabel("DIPLOMACIA BOT");
        title.setForeground(ACCENT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        JLabel subtitle = new JLabel("PC kontrol paneli");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        header.add(title, BorderLayout.WEST);
        JPanel headerRight = new JPanel(new GridLayout(2, 1, 0, 2));
        headerRight.setOpaque(false);
        JLabel author = new JLabel("Hazirlayan: @thekehribar");
        author.setForeground(MUTED);
        author.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        headerRight.add(subtitle);
        headerRight.add(author);
        header.add(headerRight, BorderLayout.EAST);

        JPanel accountTabs = new JPanel(new GridLayout(1, 2, 12, 0));
        accountTabs.setBackground(BACKGROUND);
        accountTabs.setBorder(new EmptyBorder(0, 20, 10, 20));
        account1Button = new JButton("HESAP 1");
        account2Button = new JButton("HESAP 2");
        styleButton(account1Button, ACCENT);
        styleButton(account2Button, DISABLED);
        accountTabs.add(account1Button);
        accountTabs.add(account2Button);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BACKGROUND);
        form.setBorder(new EmptyBorder(6, 20, 8, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 5, 7, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        JButton save = new JButton("Kaydet");
        JButton connect = new JButton("Baglan");
        JButton start = new JButton("Baslat");
        JButton stop = new JButton("Durdur");
        JButton donate = new JButton("Bagis yap");
        JButton addQueue = new JButton("Kuyruga ekle");
        JButton removeQueue = new JButton("Kuyruktan sil");
        JButton clear = new JButton("Loglari temizle");
        styleButton(save, BLUE);
        styleButton(connect, ACCENT);
        styleButton(start, GREEN);
        styleButton(stop, DISABLED);
        styleButton(donate, new Color(245, 178, 50));
        styleButton(addQueue, GREEN);
        styleButton(removeQueue, BLUE);
        styleButton(clear, DISABLED);

        statusLabel = new JLabel();
        statusLabel.setOpaque(true);
        statusLabel.setBackground(FIELD);
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        statusLabel.setBorder(new CompoundBorder(new LineBorder(new Color(72, 83, 106), 1, true), new EmptyBorder(14, 14, 14, 14)));
        addRow(form, c, 0, "Durum", statusLabel);

        tokenInput = new JTextArea(2, 40);
        tokenInput.setLineWrap(true);
        tokenInput.setWrapStyleWord(true);
        tokenInput.setText(config.token);
        styleTextArea(tokenInput);
        addRow(form, c, 1, "Token", tokenPanel(save, connect));

        typeButtons = new ArrayList<>();
        addRow(form, c, 2, "Odeme", optionPanel(typeButtons, TYPES, selectedType, value -> selectedType = value));

        donationAmountInput = new JSpinner(new SpinnerNumberModel(1L, 1L, Long.MAX_VALUE, 1000L));
        styleSpinner(donationAmountInput);
        addRow(form, c, 3, "Bagis", donationActionPanel(donate));

        skillButtons = new ArrayList<>();
        addRow(form, c, 4, "Beceriler", optionPanel(skillButtons, SKILLS, selectedSkill, value -> selectedSkill = value));

        queueSkillButtons = new ArrayList<>();
        addRow(form, c, 5, "Kuyruk skill", optionPanel(queueSkillButtons, SKILLS, selectedQueueSkill, value -> selectedQueueSkill = value));

        queueTypeButtons = new ArrayList<>();
        addRow(form, c, 6, "Kuyruk odeme", optionPanel(queueTypeButtons, TYPES, selectedQueueType, value -> selectedQueueType = value));

        queueCountInput = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        styleSpinner(queueCountInput);
        addRow(form, c, 7, "Kuyruk ekle", queueActionPanel(addQueue, removeQueue));

        queueModel = new DefaultListModel<>();
        for (QueueEntry entry : config.queue) {
            queueModel.addElement(entry);
        }
        queueList = new JList<>(queueModel);
        queueList.setVisibleRowCount(4);
        queueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queueList.setBackground(FIELD);
        queueList.setForeground(TEXT);
        queueList.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        queueList.setBorder(new EmptyBorder(8, 8, 8, 8));
        addRow(form, c, 8, "Emir kuyrugu", scroll(queueList));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttons.setBackground(BACKGROUND);
        buttons.setBorder(new EmptyBorder(0, 18, 14, 18));
        buttons.add(start);
        buttons.add(stop);
        buttons.add(clear);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        styleTextArea(logArea);
        logArea.setRows(6);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BACKGROUND);
        top.add(header, BorderLayout.NORTH);
        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setBackground(BACKGROUND);
        formWrap.add(accountTabs, BorderLayout.NORTH);
        formWrap.add(form, BorderLayout.CENTER);
        top.add(formWrap, BorderLayout.CENTER);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(BACKGROUND);
        logPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        JLabel logTitle = new JLabel("LOG");
        logTitle.setForeground(new Color(245, 178, 50));
        logTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        logTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        logPanel.add(logTitle, BorderLayout.NORTH);
        JScrollPane logScroll = scroll(logArea);
        logScroll.setPreferredSize(new Dimension(760, 150));
        logPanel.add(logScroll, BorderLayout.CENTER);

        frame.add(top, BorderLayout.NORTH);
        frame.add(logPanel, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);

        account1Button.addActionListener(event -> switchAccount(0));
        account2Button.addActionListener(event -> switchAccount(1));
        save.addActionListener(event -> saveConfig(false));
        connect.addActionListener(event -> connectAndCaptureToken(connect));
        donate.addActionListener(event -> donate());
        start.addActionListener(event -> {
            saveConfig(true);
            appendLog(accountName(activeAccount) + " bot baslatildi.");
            schedule(activeAccount, 0L);
        });
        stop.addActionListener(event -> stopBot(activeAccount, "Bot durduruldu."));
        addQueue.addActionListener(event -> addQueueEntry());
        removeQueue.addActionListener(event -> removeSelectedQueueEntry());
        clear.addActionListener(event -> logArea.setText(""));

        Timer timer = new Timer(1000, event -> refreshStatus());
        timer.start();
        refreshStatus();
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].enabled) {
                schedule(i, 0L);
            }
        }
        frame.setVisible(true);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component input) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        JLabel labelView = new JLabel(label);
        labelView.setForeground(TEXT);
        labelView.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        panel.add(labelView, c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(input, c);
    }

    private void switchAccount(int accountIndex) {
        if (accountIndex == activeAccount) {
            return;
        }
        saveCurrentAccount(accounts[activeAccount].enabled, false);
        activeAccount = accountIndex;
        loadActiveAccountToUi();
        updateAccountButtons();
        refreshStatus();
        appendLog(accountName(activeAccount) + " paneli acildi.");
    }

    private void loadActiveAccountToUi() {
        Config config = accounts[activeAccount];
        tokenInput.setText(config.token);
        selectedSkill = config.skill;
        selectedType = config.type;
        selectedQueueSkill = config.skill;
        selectedQueueType = config.type;
        updateOptionButtons(skillButtons, selectedSkill);
        updateOptionButtons(typeButtons, selectedType);
        updateOptionButtons(queueSkillButtons, selectedQueueSkill);
        updateOptionButtons(queueTypeButtons, selectedQueueType);
        queueModel.clear();
        for (QueueEntry entry : config.queue) {
            queueModel.addElement(entry);
        }
    }

    private void updateAccountButtons() {
        styleButton(account1Button, activeAccount == 0 ? ACCENT : DISABLED);
        styleButton(account2Button, activeAccount == 1 ? ACCENT : DISABLED);
    }

    private String accountName(int accountIndex) {
        return "Hesap " + (accountIndex + 1);
    }

    private JPanel tokenPanel(JButton save, JButton connect) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        JScrollPane tokenScroll = scroll(tokenInput);
        tokenScroll.setPreferredSize(new Dimension(420, 72));
        tokenScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 8));
        actions.setOpaque(false);
        actions.add(connect);
        actions.add(save);
        panel.add(tokenScroll, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JPanel queueActionPanel(JButton addQueue, JButton removeQueue) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        JPanel countPanel = new JPanel(new BorderLayout(6, 0));
        countPanel.setOpaque(false);
        JLabel countLabel = new JLabel("Adet");
        countLabel.setForeground(MUTED);
        countLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        countPanel.add(countLabel, BorderLayout.WEST);
        countPanel.add(queueCountInput, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.add(addQueue);
        actions.add(removeQueue);
        panel.add(countPanel, BorderLayout.WEST);
        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private JPanel donationActionPanel(JButton donate) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        JPanel amountPanel = new JPanel(new BorderLayout(6, 0));
        amountPanel.setOpaque(false);
        JLabel amountLabel = new JLabel("Miktar");
        amountLabel.setForeground(MUTED);
        amountLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        amountPanel.add(amountLabel, BorderLayout.WEST);
        amountPanel.add(donationAmountInput, BorderLayout.CENTER);
        panel.add(amountPanel, BorderLayout.CENTER);
        panel.add(donate, BorderLayout.EAST);
        return panel;
    }

    private void configureTheme() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("Spinner.background", FIELD);
        UIManager.put("TextArea.selectionBackground", ACCENT);
        UIManager.put("TextArea.selectionForeground", Color.WHITE);
    }

    private JPanel optionPanel(List<JToggleButton> buttons, String[] values, String selectedValue, Consumer<String> onSelected) {
        JPanel panel = new JPanel(new GridLayout(1, values.length, 12, 0));
        panel.setOpaque(false);
        for (String value : values) {
            JToggleButton button = new JToggleButton(displayName(value));
            button.putClientProperty("value", value);
            buttons.add(button);
            styleOptionButton(button, value.equals(selectedValue));
            button.addActionListener(event -> {
                onSelected.accept(value);
                updateOptionButtons(buttons, value);
            });
            panel.add(button);
        }
        return panel;
    }

    private void updateOptionButtons(List<JToggleButton> buttons, String selectedValue) {
        for (JToggleButton button : buttons) {
            String value = String.valueOf(button.getClientProperty("value"));
            styleOptionButton(button, value.equals(selectedValue));
        }
    }

    private void styleOptionButton(JToggleButton button, boolean selected) {
        button.setSelected(selected);
        button.setBackground(selected ? CARD : FIELD);
        button.setForeground(selected ? TEXT : MUTED);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        button.setPreferredSize(new Dimension(130, 46));
        button.setBorder(new CompoundBorder(new LineBorder(selected ? ACCENT : new Color(50, 65, 92), 1, true), new EmptyBorder(10, 12, 10, 12)));
    }

    private String displayName(String value) {
        return switch (value) {
            case "money" -> "Para";
            case "diamond" -> "Elmas";
            case "kisla" -> "Kisla";
            case "savas_teknikleri" -> "Savas";
            case "bilim_insani" -> "Bilim";
            default -> value;
        };
    }

    private JScrollPane scroll(Component component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(new LineBorder(new Color(35, 47, 70), 1, true));
        pane.getViewport().setBackground(FIELD);
        pane.setBackground(FIELD);
        return pane;
    }

    private void styleTextArea(JTextArea area) {
        area.setBackground(FIELD);
        area.setForeground(TEXT);
        area.setCaretColor(TEXT);
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setPreferredSize(new Dimension(90, 44));
        spinner.setBorder(new CompoundBorder(new LineBorder(new Color(50, 65, 92), 1, true), new EmptyBorder(4, 8, 4, 8)));
        JComponent editor = spinner.getEditor();
        editor.setBackground(FIELD);
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(FIELD);
            defaultEditor.getTextField().setForeground(TEXT);
            defaultEditor.getTextField().setCaretColor(TEXT);
            defaultEditor.getTextField().setHorizontalAlignment(javax.swing.JTextField.CENTER);
            defaultEditor.getTextField().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            defaultEditor.getTextField().setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        button.setPreferredSize(new Dimension(Math.max(126, button.getPreferredSize().width + 28), 44));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    private void connectAndCaptureToken(JButton connectButton) {
        connectButton.setEnabled(false);
        int accountIndex = activeAccount;
        appendLog(accountName(accountIndex) + " baglan aciliyor. Temiz profilli normal Chrome acilacak.");
        Thread thread = new Thread(() -> {
            try {
                String token = openDebugChromeAndCaptureToken(accountIndex);
                if (token == null) {
                    appendLog(accountName(accountIndex) + " token yakalanamadi. Girisi tamamlayip tekrar deneyebilir veya response/header metnini Token alanina yapistirabilirsin.");
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    accounts[accountIndex] = accounts[accountIndex].withToken(token);
                    accounts[accountIndex].save(accountIndex);
                    if (activeAccount == accountIndex) {
                        tokenInput.setText(token);
                    }
                    appendLog(accountName(accountIndex) + " token yakalandi ve kaydedildi.");
                });
            } catch (Exception error) {
                appendLog(accountName(accountIndex) + " baglan hatasi: " + firstLine(error.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> connectButton.setEnabled(true));
            }
        }, "diplomacia-chrome-cdp");
        thread.setDaemon(true);
        thread.start();
    }

    private String openDebugChromeAndCaptureToken(int accountIndex) throws Exception {
        Path chromePath = findChromeExecutable();
        if (chromePath == null) {
            openChrome();
            throw new IllegalStateException("Chrome yolu bulunamadi. Chrome acildiyse token response/header metnini elle yapistir.");
        }

        int port = freePort();
        Path profileDir = CONFIG_PATH.getParent().resolve(".diplomacia-clean-chrome-profile-account" + (accountIndex + 1));
        Files.createDirectories(profileDir);
        new ProcessBuilder(
                chromePath.toString(),
                "--remote-debugging-port=" + port,
                "--user-data-dir=" + profileDir,
                "--no-first-run",
                "--new-window",
                DEFAULT_BASE_URL
        ).start();
        appendLog(accountName(accountIndex) + " temiz Chrome acildi. Google ile giris yap, token dinleniyor.");

        String webSocketUrl = waitForPageWebSocket(port);
        if (webSocketUrl == null) {
            throw new IllegalStateException("Chrome DevTools baglantisi bulunamadi.");
        }
        return captureTokenFromDevTools(webSocketUrl);
    }

    private Path findChromeExecutable() {
        for (Path chromePath : chromePaths()) {
            if (Files.exists(chromePath)) {
                return chromePath;
            }
        }
        return null;
    }

    private int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private String waitForPageWebSocket(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/json")).GET().build();
                String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
                String webSocketUrl = parseFirstPageWebSocket(body);
                if (webSocketUrl != null) {
                    return webSocketUrl;
                }
            } catch (Exception ignored) {
                Thread.sleep(500L);
            }
        }
        return null;
    }

    private String parseFirstPageWebSocket(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            org.json.JSONArray pages = new org.json.JSONArray(body);
            for (int i = 0; i < pages.length(); i++) {
                JSONObject page = pages.getJSONObject(i);
                if (!"page".equals(page.optString("type"))) {
                    continue;
                }
                String url = page.optString("webSocketDebuggerUrl", "");
                if (!url.isBlank()) {
                    return url;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String captureTokenFromDevTools(String webSocketUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicInteger messageId = new AtomicInteger(1);
        CountDownLatch done = new CountDownLatch(1);
        Map<Integer, String> responseRequests = new HashMap<>();

        WebSocket webSocket = client.newWebSocketBuilder().buildAsync(URI.create(webSocketUrl), new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                buffer.append(data);
                if (!last) {
                    return WebSocket.Listener.super.onText(webSocket, data, false);
                }
                handleDevToolsMessage(webSocket, buffer.toString(), token, done, responseRequests, messageId);
                buffer.setLength(0);
                return WebSocket.Listener.super.onText(webSocket, data, true);
            }
        }).join();

        sendDevTools(webSocket, messageId.getAndIncrement(), "Network.enable", new JSONObject());
        boolean found = done.await(5, TimeUnit.MINUTES);
        webSocket.abort();
        return found ? token.get() : null;
    }

    private void handleDevToolsMessage(WebSocket webSocket, String message, AtomicReference<String> token, CountDownLatch done, Map<Integer, String> responseRequests, AtomicInteger messageId) {
        try {
            JSONObject json = new JSONObject(message);
            String method = json.optString("method", "");
            JSONObject params = json.optJSONObject("params");
            if (params == null) {
                if (json.has("id") && json.has("result")) {
                    String body = json.getJSONObject("result").optString("body", "");
                    String found = extractTokenFromAuthResponse(body);
                    if (found != null && token.compareAndSet(null, found)) {
                        appendLog("Token /api/auth/google cevabindan yakalandi.");
                        done.countDown();
                    }
                }
                return;
            }

            if ("Network.requestWillBeSentExtraInfo".equals(method)) {
                JSONObject headers = params.optJSONObject("headers");
                String found = headers == null ? null : extractToken(headers.optString("authorization", headers.optString("Authorization", "")));
                if (found != null && token.compareAndSet(null, found)) {
                    appendLog("Token authorization header'indan yakalandi.");
                    done.countDown();
                }
                return;
            }

            if ("Network.responseReceived".equals(method)) {
                JSONObject response = params.optJSONObject("response");
                String url = response == null ? "" : response.optString("url", "");
                if (url.contains("/api/auth/google")) {
                    int id = messageId.getAndIncrement();
                    responseRequests.put(id, params.optString("requestId", ""));
                    JSONObject commandParams = new JSONObject().put("requestId", params.optString("requestId", ""));
                    sendDevTools(webSocket, id, "Network.getResponseBody", commandParams);
                }
            }
        } catch (Exception ignored) {
            // Ignore non-JSON or partial DevTools messages.
        }
    }

    private void sendDevTools(WebSocket webSocket, int id, String method, JSONObject params) {
        JSONObject command = new JSONObject()
                .put("id", id)
                .put("method", method)
                .put("params", params);
        webSocket.sendText(command.toString(), true);
    }

    private void openChrome() {
        for (Path chromePath : chromePaths()) {
            if (!Files.exists(chromePath)) {
                continue;
            }
            try {
                new ProcessBuilder(chromePath.toString(), DEFAULT_BASE_URL).start();
                appendLog("Chrome acildi: " + chromePath);
                return;
            } catch (Exception error) {
                appendLog("Chrome acma hatasi: " + firstLine(error.getMessage()));
            }
        }

        try {
            new ProcessBuilder("cmd", "/c", "start", "", "chrome", DEFAULT_BASE_URL).start();
            appendLog("Chrome komutu ile acma denendi.");
        } catch (Exception error) {
            appendLog("Chrome bulunamadi: " + firstLine(error.getMessage()));
            appendLog("Chrome kurulu degilse Token alanina header/response yapistirarak devam edebilirsin.");
        }
    }

    private List<Path> chromePaths() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        List<Path> paths = new ArrayList<>();
        if (localAppData != null) {
            paths.add(Paths.get(localAppData, "Google", "Chrome", "Application", "chrome.exe"));
        }
        if (programFiles != null) {
            paths.add(Paths.get(programFiles, "Google", "Chrome", "Application", "chrome.exe"));
        }
        if (programFilesX86 != null) {
            paths.add(Paths.get(programFilesX86, "Google", "Chrome", "Application", "chrome.exe"));
        }
        return paths;
    }

    private String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "bilinmeyen hata";
        }
        return text.split("\\R", 2)[0];
    }

    private static String extractToken(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher bearer = BEARER_PATTERN.matcher(text);
        if (bearer.find()) {
            String candidate = bearer.group(1);
            return isJwt(candidate) ? candidate : null;
        }
        Matcher jwt = JWT_PATTERN.matcher(text);
        if (jwt.find()) {
            return jwt.group();
        }
        return null;
    }

    private static String extractTokenFromAuthResponse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(body);
            String token = json.optString("token", "");
            if (isJwt(token)) {
                return token;
            }
        } catch (Exception ignored) {
            // Fall back to scanning text below.
        }
        return extractToken(body);
    }

    private static boolean isJwt(String value) {
        return value != null && JWT_PATTERN.matcher(value).matches();
    }

    private static String normalizeToken(String text) {
        String token = extractToken(text);
        String trimmed = text == null ? "" : text.trim();
        if (token != null) {
            return token;
        }
        if (trimmed.equalsIgnoreCase("diplomacia.com.tr") || trimmed.equalsIgnoreCase(DEFAULT_BASE_URL)) {
            return "";
        }
        return trimmed;
    }

    private void saveConfig(boolean enabledValue) {
        saveCurrentAccount(enabledValue, true);
        appendLog(accountName(activeAccount) + " ayarlari kaydedildi.");
        refreshStatus();
    }

    private void saveCurrentAccount(boolean enabledValue, boolean saveToDisk) {
        accounts[activeAccount] = readConfigFromUi(enabledValue);
        if (saveToDisk) {
            accounts[activeAccount].save(activeAccount);
        }
    }

    private Config readConfigFromUi(boolean enabledValue) {
        String token = normalizeToken(tokenInput.getText());
        if (!token.equals(tokenInput.getText().trim())) {
            tokenInput.setText(token);
            appendLog("Token metinden ayiklandi.");
        }
        return new Config(
                token,
                DEFAULT_BASE_URL,
                selectedSkill,
                selectedType,
                DEFAULT_BUFFER_MS,
                enabledValue,
                queueEntries()
        );
    }

    private void addQueueEntry() {
        QueueEntry entry = new QueueEntry(
                selectedQueueSkill,
                selectedQueueType,
                ((Number) queueCountInput.getValue()).intValue()
        );
        queueModel.addElement(entry);
        saveConfig(accounts[activeAccount].enabled);
        appendLog(accountName(activeAccount) + " kuyruga eklendi: " + entry);
    }

    private void removeSelectedQueueEntry() {
        int index = queueList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        QueueEntry removed = queueModel.remove(index);
        saveConfig(accounts[activeAccount].enabled);
        appendLog(accountName(activeAccount) + " kuyruktan silindi: " + removed);
    }

    private void donate() {
        long amount = ((Number) donationAmountInput.getValue()).longValue();
        if (amount <= 0L) {
            appendLog(accountName(activeAccount) + " bagis hatasi: miktar 0'dan buyuk olmali.");
            return;
        }

        saveCurrentAccount(accounts[activeAccount].enabled, true);
        Config config = accounts[activeAccount];
        if (config.token.isBlank()) {
            appendLog(accountName(activeAccount) + " bagis hatasi: once token kaydet.");
            return;
        }

        int accountIndex = activeAccount;
        appendLog(accountName(accountIndex) + " bagis gonderiliyor: " + amount);
        executor.execute(() -> {
            try {
                ApiResult result = donate(config, amount);
                if (result.isSuccessful() && result.json.optBoolean("success", false)) {
                    String recipient = result.json.optString("recipient_username", "alici");
                    long sentAmount = result.json.optLong("amount", amount);
                    long newBalance = result.json.optLong("new_balance", -1L);
                    String message = "bagis basarili: " + sentAmount + " -> " + recipient;
                    if (newBalance >= 0L) {
                        message += " | Yeni bakiye: " + newBalance;
                    }
                    appendLog(accountName(accountIndex) + " " + message);
                    return;
                }
                String error = result.json.optString("error", result.raw.isEmpty() ? "HTTP " + result.status : result.raw);
                appendLog(accountName(accountIndex) + " bagis basarisiz: " + error);
            } catch (Exception error) {
                appendLog(accountName(accountIndex) + " bagis hatasi: " + error.getMessage());
            }
        });
    }

    private List<QueueEntry> queueEntries() {
        List<QueueEntry> entries = new ArrayList<>();
        for (int i = 0; i < queueModel.size(); i++) {
            entries.add(queueModel.get(i));
        }
        return entries;
    }

    private void schedule(int accountIndex, long delayMs) {
        if (scheduledRuns[accountIndex] != null) {
            scheduledRuns[accountIndex].cancel(false);
        }
        nextRunAt[accountIndex] = System.currentTimeMillis() + delayMs;
        scheduledRuns[accountIndex] = executor.schedule(() -> runOnce(accountIndex), Math.max(delayMs, 0L), TimeUnit.MILLISECONDS);
        refreshStatus();
    }

    private void runOnce(int accountIndex) {
        Config savedConfig = accounts[accountIndex];
        if (!savedConfig.isReady()) {
            appendLog(accountName(accountIndex) + " bot durdu veya token bos.");
            return;
        }

        try {
            BotCommand command = nextCommand(savedConfig);
            Config runConfig = savedConfig.withCommand(command.skill, command.type);
            appendLog(accountName(accountIndex) + " istek atiliyor: " + runConfig.skill + " (" + runConfig.type + ")");
            ApiResult result = upgrade(runConfig);
            long waitMs = computeWaitMs(result.json, runConfig.bufferMs);

            if (result.isSuccessful()) {
                appendLog(accountName(accountIndex) + " OK: " + summarizeSuccess(result.json));
                if (command.queued) {
                    consumeQueueEntry(accountIndex);
                }
            } else if (result.status == 429) {
                appendLog(accountName(accountIndex) + " bekleme gerekli: " + result.raw);
            } else if (result.status == 401) {
                stopBot(accountIndex, "Token gecersiz veya suresi dolmus. Bot durduruldu.");
                return;
            } else {
                appendLog(accountName(accountIndex) + " HTTP " + result.status + ": " + result.raw);
            }

            if (accounts[accountIndex].enabled) {
                schedule(accountIndex, Math.max(waitMs, 60000L));
            }
        } catch (Exception error) {
            appendLog(accountName(accountIndex) + " hata: " + error.getMessage());
            if (accounts[accountIndex].enabled) {
                schedule(accountIndex, TimeUnit.MINUTES.toMillis(5));
            }
        }
    }

    private BotCommand nextCommand(Config savedConfig) {
        if (!savedConfig.queue.isEmpty()) {
            QueueEntry entry = savedConfig.queue.get(0);
            return new BotCommand(entry.skill, entry.type, true);
        }
        return new BotCommand(savedConfig.skill, savedConfig.type, false);
    }

    private void consumeQueueEntry(int accountIndex) {
        SwingUtilities.invokeLater(() -> {
            List<QueueEntry> queue = new ArrayList<>(accounts[accountIndex].queue);
            if (queue.isEmpty()) {
                return;
            }
            QueueEntry current = queue.get(0);
            if (current.count <= 1) {
                queue.remove(0);
                appendLog(accountName(accountIndex) + " kuyruk emri tamamlandi: " + current.skill + " (" + current.type + ")");
            } else {
                queue.set(0, new QueueEntry(current.skill, current.type, current.count - 1));
            }
            accounts[accountIndex] = accounts[accountIndex].withQueue(queue);
            accounts[accountIndex].save(accountIndex);
            if (activeAccount == accountIndex) {
                queueModel.clear();
                for (QueueEntry entry : queue) {
                    queueModel.addElement(entry);
                }
            }
        });
    }

    private void stopBot(int accountIndex, String message) {
        if (activeAccount == accountIndex) {
            accounts[accountIndex] = readConfigFromUi(false);
        } else {
            accounts[accountIndex] = accounts[accountIndex].withEnabled(false);
        }
        accounts[accountIndex].save(accountIndex);
        if (scheduledRuns[accountIndex] != null) {
            scheduledRuns[accountIndex].cancel(false);
        }
        nextRunAt[accountIndex] = 0L;
        appendLog(accountName(accountIndex) + " " + message);
        refreshStatus();
    }

    private void refreshStatus() {
        if (statusLabel == null) {
            return;
        }
        Config config = accounts[activeAccount];
        long remainingMs = nextRunAt[activeAccount] - System.currentTimeMillis();
        String next = config.enabled && remainingMs > 0L
                ? new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(nextRunAt[activeAccount]))
                : "hazir";
        String countdown = config.enabled && remainingMs > 0L ? " | Kalan: " + formatDuration(remainingMs) : "";
        statusLabel.setText(accountName(activeAccount) + " | " + (config.enabled ? "calisiyor" : "durdu") + " | Sonraki: " + next + countdown);
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            logArea.append("[" + time + "] " + message + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static ApiResult upgrade(Config config) throws Exception {
        URL url = new URL(config.baseUrl + "/api/players/skills/upgrade");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + config.token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Origin", config.baseUrl);

        JSONObject body = new JSONObject();
        body.put("skill", config.skill);
        body.put("type", config.type);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }

        int status = connection.getResponseCode();
        String text = readResponse(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
        JSONObject json = text.trim().isEmpty() ? new JSONObject() : new JSONObject(text);
        return new ApiResult(status, json, text);
    }

    private static ApiResult donate(Config config, long amount) throws Exception {
        URL url = new URL(config.baseUrl + "/api/transfer/send");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + config.token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Origin", config.baseUrl);

        JSONObject body = new JSONObject();
        body.put("recipient_id", DONATION_RECIPIENT_ID);
        body.put("amount", amount);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }

        int status = connection.getResponseCode();
        String text = readResponse(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
        JSONObject json = text.trim().isEmpty() ? new JSONObject() : new JSONObject(text);
        return new ApiResult(status, json, text);
    }

    private static String readResponse(InputStream input) throws Exception {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
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

    private record Config(String token, String baseUrl, String skill, String type, long bufferMs, boolean enabled, List<QueueEntry> queue) {
        static Config load(int accountIndex) {
            Properties props = new Properties();
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                    props.load(input);
                } catch (Exception ignored) {
                    // Defaults are safer than failing the app at startup.
                }
            }

            String prefix = prefix(accountIndex);
            boolean legacy = accountIndex == 0 && props.getProperty(prefix + "token") == null;

            return new Config(
                    normalizeToken(props.getProperty(legacy ? "token" : prefix + "token", "")),
                    DEFAULT_BASE_URL,
                    props.getProperty(legacy ? "skill" : prefix + "skill", SKILLS[2]),
                    props.getProperty(legacy ? "type" : prefix + "type", TYPES[0]),
                    DEFAULT_BUFFER_MS,
                    Boolean.parseBoolean(props.getProperty(legacy ? "enabled" : prefix + "enabled", "false")),
                    parseQueue(props.getProperty(legacy ? "queue" : prefix + "queue", ""))
            );
        }

        Config {
            baseUrl = trimTrailingSlash(baseUrl == null || baseUrl.trim().isEmpty() ? DEFAULT_BASE_URL : baseUrl.trim());
            skill = skill == null || skill.trim().isEmpty() ? SKILLS[2] : skill.trim();
            type = type == null || type.trim().isEmpty() ? TYPES[0] : type.trim();
            bufferMs = Math.max(bufferMs, 0L);
            queue = List.copyOf(queue == null ? List.of() : queue);
        }

        Config withCommand(String commandSkill, String commandType) {
            return new Config(token, baseUrl, commandSkill, commandType, bufferMs, enabled, queue);
        }

        Config withToken(String newToken) {
            return new Config(newToken, baseUrl, skill, type, bufferMs, enabled, queue);
        }

        Config withQueue(List<QueueEntry> newQueue) {
            return new Config(token, baseUrl, skill, type, bufferMs, enabled, newQueue);
        }

        Config withEnabled(boolean newEnabled) {
            return new Config(token, baseUrl, skill, type, bufferMs, newEnabled, queue);
        }

        void save(int accountIndex) {
            Properties props = new Properties();
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                    props.load(input);
                } catch (Exception ignored) {
                    // Continue with a clean config file if the old file is broken.
                }
            }
            String prefix = prefix(accountIndex);
            props.setProperty(prefix + "token", token == null ? "" : token);
            props.setProperty(prefix + "skill", skill);
            props.setProperty(prefix + "type", type);
            props.setProperty(prefix + "enabled", String.valueOf(enabled));
            props.setProperty(prefix + "queue", formatQueue(queue));
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                props.store(output, "Diplomacia Bot Desktop");
            } catch (Exception error) {
                throw new IllegalStateException("Ayarlar kaydedilemedi: " + error.getMessage(), error);
            }
        }

        boolean isReady() {
            return enabled && token != null && !token.trim().isEmpty();
        }

        private static String trimTrailingSlash(String value) {
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }

        private static String prefix(int accountIndex) {
            return "account" + (accountIndex + 1) + ".";
        }

        private static List<QueueEntry> parseQueue(String value) {
            List<QueueEntry> entries = new ArrayList<>();
            if (value == null || value.isBlank()) {
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
                    // Skip broken saved queue rows.
                }
            }
            return entries;
        }

        private static String formatQueue(List<QueueEntry> queue) {
            StringBuilder builder = new StringBuilder();
            for (QueueEntry entry : queue) {
                if (!builder.isEmpty()) {
                    builder.append(';');
                }
                builder.append(entry.skill).append(',').append(entry.type).append(',').append(entry.count);
            }
            return builder.toString();
        }
    }

    private record QueueEntry(String skill, String type, int count) {
        QueueEntry {
            skill = skill == null || skill.trim().isEmpty() ? SKILLS[2] : skill.trim();
            type = type == null || type.trim().isEmpty() ? TYPES[0] : type.trim();
            count = Math.max(1, count);
        }

        @Override
        public String toString() {
            return count + "x " + skill + " (" + type + ")";
        }
    }

    private record BotCommand(String skill, String type, boolean queued) {
    }

    private record ApiResult(int status, JSONObject json, String raw) {
        boolean isSuccessful() {
            return status >= 200 && status < 300;
        }
    }
}
