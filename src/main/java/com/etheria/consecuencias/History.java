package com.etheria.consecuencias;

import org.bukkit.Bukkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Historial de desastres (historial.log): qué pasó, cuándo, dónde y quién lo lanzó. */
public class History {
    private final ConsecuenciasPlugin pl;
    private final File file;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public History(ConsecuenciasPlugin pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "historial.log");
    }

    public void add(String line) {
        final String l = fmt.format(new Date()) + " | " + Util.strip(line);
        Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            synchronized (this) {
                try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    w.write(l);
                    w.newLine();
                } catch (IOException ignored) {
                }
            }
        });
    }

    public synchronized List<String> tail(int n) {
        List<String> out = new ArrayList<>();
        if (!file.exists()) return out;
        try {
            List<String> all = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (int i = Math.max(0, all.size() - n); i < all.size(); i++) out.add(all.get(i));
        } catch (IOException ignored) {
        }
        return out;
    }
}
