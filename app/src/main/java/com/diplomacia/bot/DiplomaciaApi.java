package com.diplomacia.bot;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class DiplomaciaApi {
    static final String DONATION_RECIPIENT_ID = "ec756c8c-d06a-474f-973e-6fdec9cb58c6";

    static ApiResult upgrade(BotConfig config) throws Exception {
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

    static ApiResult donate(BotConfig config, long amount) throws Exception {
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

    static String readResponse(InputStream input) throws Exception {
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

    static final class ApiResult {
        final int status;
        final JSONObject json;
        final String raw;

        ApiResult(int status, JSONObject json, String raw) {
            this.status = status;
            this.json = json;
            this.raw = raw;
        }

        boolean isSuccessful() {
            return status >= 200 && status < 300;
        }
    }
}
