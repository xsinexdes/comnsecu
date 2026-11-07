package com.etheria.consecuencias;

import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Aviso opcional a un canal de Discord (webhook). */
public final class Webhook {
    private Webhook() {
    }

    public static void send(ConsecuenciasPlugin plugin, String text) {
        final String url = plugin.getConfig().getString("discord.webhook", "").trim();
        if (url.isEmpty() || !url.startsWith("https://")) return;
        final String clean = Util.strip(text);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("User-Agent", "Consecuencias");
                String json = "{\"content\":\"" + clean.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
                try (OutputStream o = c.getOutputStream()) {
                    o.write(json.getBytes(StandardCharsets.UTF_8));
                }
                c.getResponseCode();
                c.disconnect();
            } catch (Exception ignored) {
            }
        });
    }
}
