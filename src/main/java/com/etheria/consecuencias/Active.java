package com.etheria.consecuencias;

import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Un desastre en curso en un mundo concreto. */
public class Active {
    public final int id;
    public final Disaster d;
    public final World world;
    public final int level;          // 1..3
    public final int minutes;
    public final long created;
    public long warnUntil;           // hasta cuándo dura el aviso previo
    public long end;                 // fin del desastre (ms)
    public boolean running;
    public int beat;                 // latidos desde que empezó (cada 2 ticks)
    public BossBar bar;
    public final Random rnd = new Random();
    public final Map<String, Object> data = new HashMap<>();
    public String by = "auto";
    /** Cicatriz: los bloques cambiados NO se restauran al terminar (terreno cambiado para siempre). */
    public boolean scar;
    public final List<org.bukkit.entity.Entity> mobs = new ArrayList<>();
    public final java.util.Set<java.util.UUID> present = new java.util.HashSet<>();
    public final java.util.Set<java.util.UUID> died = new java.util.HashSet<>();

    public Active(int id, Disaster d, World world, int level, int minutes) {
        this.id = id;
        this.d = d;
        this.world = world;
        this.level = Math.max(1, Math.min(3, level));
        this.minutes = minutes;
        this.created = System.currentTimeMillis();
    }

    public long msLeft() {
        return Math.max(0, end - System.currentTimeMillis());
    }

    /** 0 al empezar, 1 al terminar. */
    public double progress() {
        long total = Math.max(1, end - (warnUntil));
        return Math.min(1.0, Math.max(0.0, (System.currentTimeMillis() - warnUntil) / (double) total));
    }

    /** Jugadores del mundo que sufren los efectos (no creativo/espectador/inmunes). */
    public List<Player> players() {
        List<Player> out = new ArrayList<>();
        for (Player p : world.getPlayers()) if (Util.valid(p)) out.add(p);
        return out;
    }

    /** Criaturas vivas creadas por este desastre. */
    public int mobCount() {
        mobs.removeIf(e -> !e.isValid());
        return mobs.size();
    }

    public int scale(int base) {
        return Math.max(1, base * level);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String k, T def) {
        Object o = data.get(k);
        return o == null ? def : (T) o;
    }
}
