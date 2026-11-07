package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Supervivientes y muertes por desastres (stats.yml). */
public class Stats {
    private final ConsecuenciasPlugin pl;
    private final File file;
    private final Map<UUID, int[]> data = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

    public Stats(ConsecuenciasPlugin pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "stats.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            var s = y.getConfigurationSection("jugadores");
            if (s != null) for (String k : s.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    data.put(u, new int[]{s.getInt(k + ".superados"), s.getInt(k + ".muertes")});
                    names.put(u, s.getString(k + ".nombre", k));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private int[] of(UUID u, String name) {
        names.put(u, name);
        return data.computeIfAbsent(u, k -> new int[2]);
    }

    public void survived(UUID u, String name) {
        of(u, name)[0]++;
        save();
    }

    public void died(UUID u, String name) {
        of(u, name)[1]++;
        save();
    }

    public int[] get(UUID u) {
        return data.getOrDefault(u, new int[2]);
    }

    public List<Map.Entry<UUID, int[]>> top(int n) {
        List<Map.Entry<UUID, int[]>> l = new ArrayList<>(data.entrySet());
        l.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
        return l.size() > n ? l.subList(0, n) : l;
    }

    public String name(UUID u) {
        return names.getOrDefault(u, u.toString().substring(0, 8));
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (var e : data.entrySet()) {
            String k = "jugadores." + e.getKey();
            y.set(k + ".nombre", names.get(e.getKey()));
            y.set(k + ".superados", e.getValue()[0]);
            y.set(k + ".muertes", e.getValue()[1]);
        }
        Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            try {
                y.save(file);
            } catch (IOException ex) {
                pl.getLogger().warning("No se pudo guardar stats.yml: " + ex.getMessage());
            }
        });
    }
}
