package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class DisasterManager {
    private final ConsecuenciasPlugin pl;
    private final Map<String, Disaster> reg = new LinkedHashMap<>();
    private final List<Active> actives = new CopyOnWriteArrayList<>();
    private int nextId = 1;
    private BukkitTask task;
    private long nextAuto;
    private final Random rnd = new Random();

    public DisasterManager(ConsecuenciasPlugin pl) {
        this.pl = pl;
    }

    public void register(Disaster d) {
        reg.put(d.id, d);
    }

    public Disaster get(String id) {
        return reg.get(id.toLowerCase());
    }

    public java.util.Collection<Disaster> all() {
        return reg.values();
    }

    public List<Active> actives() {
        return actives;
    }

    public List<Active> of(World w) {
        List<Active> out = new ArrayList<>();
        for (Active a : actives) if (a.world.equals(w)) out.add(a);
        return out;
    }

    public Active find(String id, World w) {
        for (Active a : actives) if (a.d.id.equals(id) && a.world.equals(w) && a.running) return a;
        return null;
    }

    public boolean has(World w, String... ids) {
        for (Active a : actives) {
            if (!a.running || !a.world.equals(w)) continue;
            for (String id : ids) if (a.d.id.equals(id)) return true;
        }
        return false;
    }

    public boolean enabled(Disaster d) {
        return pl.getConfig().getBoolean("desastres." + d.id + ".activo", true);
    }

    public int minutes(Disaster d) {
        return Math.max(1, pl.getConfig().getInt("desastres." + d.id + ".minutos", d.defMinutes));
    }

    public int weight(Disaster d) {
        return pl.getConfig().getInt("desastres." + d.id + ".peso", 10);
    }

    public List<World> worlds() {
        List<World> out = new ArrayList<>();
        List<String> names = pl.getConfig().getStringList("mundos");
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() != World.Environment.NORMAL) continue;
            if (names.isEmpty() || names.contains(w.getName())) out.add(w);
        }
        return out;
    }

    // ------------------------------------------------------------------ activar / detener

    /** null = ok; si no, el motivo por el que no se pudo. */
    public String start(Disaster d, World w, int minutes, int level, int warnSeconds, String by, boolean force) {
        return start(d, w, minutes, level, warnSeconds, by, force, false);
    }

    public String start(Disaster d, World w, int minutes, int level, int warnSeconds, String by, boolean force, boolean scar) {
        if (w == null) return "Ese mundo no existe.";
        if (!d.worldOk(w)) return d.name + " no se puede dar en ese mundo.";
        for (Active a : actives) {
            if (a.world.equals(w) && a.d == d) return d.name + " ya está en marcha en " + w.getName() + ".";
            if (a.world.equals(w) && (d.conflicts().contains(a.d.id) || a.d.conflicts().contains(d.id))) {
                return d.name + " es incompatible con " + a.d.name + " (ya en curso).";
            }
        }
        if (!force) {
            int max = pl.getConfig().getInt("auto.max-simultaneos", 2);
            if (of(w).size() >= max) return "Ya hay " + max + " desastres en ese mundo.";
        }
        Active a = new Active(nextId++, d, w, level, Math.max(1, minutes));
        a.by = by;
        a.scar = scar;
        warnSeconds = Math.max(0, warnSeconds);
        a.warnUntil = System.currentTimeMillis() + warnSeconds * 1000L;
        a.end = a.warnUntil + a.minutes * 60_000L;
        actives.add(a);
        if (pl.getConfig().getBoolean("anuncios.bossbar", true)) {
            BossBar b = Bukkit.createBossBar(d.color + d.name, colorOf(d.category), BarStyle.SEGMENTED_10);
            a.bar = b;
            for (Player p : w.getPlayers()) b.addPlayer(p);
        }
        if (warnSeconds > 0) Announcer.warn(pl, a, warnSeconds);
        else activate(a);
        pl.getLogger().info("Desastre " + d.id + " (" + by + ") en " + w.getName() + ": " + a.minutes + " min, nivel " + a.level + ".");
        pl.history.add("INICIO " + d.name + " | mundo " + w.getName() + " | " + a.minutes + " min | nivel " + a.level + " | por " + by);
        return null;
    }

    private BarColor colorOf(String cat) {
        return switch (cat) {
            case "Clima" -> BarColor.BLUE;
            case "Tierra" -> BarColor.YELLOW;
            case "Cielo" -> BarColor.PURPLE;
            case "Vida" -> BarColor.GREEN;
            case "Bendición" -> BarColor.PINK;
            default -> BarColor.RED;
        };
    }

    private void activate(Active a) {
        a.running = true;
        for (Player p : a.players()) a.present.add(p.getUniqueId());
        a.warnUntil = System.currentTimeMillis();
        a.end = System.currentTimeMillis() + a.minutes * 60_000L;
        try {
            a.d.start(a);
        } catch (Throwable t) {
            pl.getLogger().warning("Error al iniciar " + a.d.id + ": " + t);
            stop(a, false);
            return;
        }
        Announcer.begin(pl, a);
    }

    public void stop(Active a, boolean natural) {
        if (!actives.remove(a)) return;
        try {
            if (a.running) a.d.stop(a);
        } catch (Throwable t) {
            pl.getLogger().warning("Error al detener " + a.d.id + ": " + t);
        }
        // quita las criaturas que creó el desastre
        cleanup(a);
        if (a.bar != null) {
            a.bar.removeAll();
            a.bar = null;
        }
        pl.ledger.stat.remove(a.id);
        if (a.scar) pl.ledger.forget(a);
        else pl.ledger.release(a, pl.getConfig().getLong("regenerar-segundos", 20) * 1000L);
        if (a.running) Announcer.end(pl, a, natural);
        if (a.running) pl.history.add((natural ? "FIN " : "DETENIDO ") + a.d.name + " | mundo " + a.world.getName() + (a.died.isEmpty() ? "" : " | muertes: " + a.died.size()));
        if (a.running && natural && !a.d.blessing()) rewardSurvivors(a);
        if (a.running && natural) chain(a);
    }

    /** Cadenas: un desastre puede desencadenar otro al terminar (config → cadenas). */
    private void chain(Active a) {
        if (!pl.getConfig().getBoolean("cadenas.activo", true)) return;
        for (Map<?, ?> r : pl.getConfig().getMapList("cadenas.reglas")) {
            if (!a.d.id.equals(String.valueOf(r.get("origen")))) continue;
            int prob = r.get("probabilidad") instanceof Number n ? n.intValue() : 30;
            if (rnd.nextInt(100) >= prob) continue;
            Disaster dest = get(String.valueOf(r.get("destino")));
            if (dest == null || !enabled(dest)) continue;
            int delay = r.get("retraso-segundos") instanceof Number n ? n.intValue() : 30;
            final World w = a.world;
            final int level = a.level;
            final String from = a.d.name;
            Bukkit.getScheduler().runTaskLater(pl, () -> {
                String err = start(dest, w, minutes(dest), level, pl.getConfig().getInt("aviso-segundos", 15), "cadena de " + from, true);
                if (err == null) for (Player p : w.getPlayers()) p.sendMessage("§8⛓ §7Como consecuencia de §f" + from + "§7...");
            }, Math.max(1, delay) * 20L);
        }
    }

    /** Premia a quien aguantó todo el desastre sin morir. */
    private void rewardSurvivors(Active a) {
        int n = 0;
        long xp = pl.getConfig().getLong("recompensas.xp", 40);
        List<String> cmds = pl.getConfig().getStringList("recompensas.comandos");
        for (java.util.UUID u : a.present) {
            Player p = Bukkit.getPlayer(u);
            if (p == null || !p.isOnline() || !p.getWorld().equals(a.world) || a.died.contains(u)) continue;
            n++;
            pl.stats.survived(u, p.getName());
            if (xp > 0) p.giveExp((int) (xp * a.level));
            for (String c : cmds) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("{jugador}", p.getName()).replace("{desastre}", a.d.name).replace("{nivel}", String.valueOf(a.level)));
            p.sendMessage("§a🛡 §7Has sobrevivido a §f" + a.d.name + "§7. " + (xp > 0 ? "§e+" + (xp * a.level) + " XP" : ""));
        }
        if (!a.present.isEmpty()) {
            for (Player p : a.world.getPlayers()) p.sendMessage("§a🛡 §7Supervivientes de §f" + a.d.name + "§7: §a" + n + "§7/§f" + a.present.size());
        }
    }

    private void cleanup(Active a) {
        for (Entity e : new ArrayList<>(a.mobs)) e.remove();
        a.mobs.clear();
    }

    public int stopAll() {
        int n = 0;
        for (Active a : new ArrayList<>(actives)) {
            stop(a, false);
            n++;
        }
        return n;
    }

    public int stop(String id, World w) {
        int n = 0;
        for (Active a : new ArrayList<>(actives)) {
            if (a.d.id.equals(id) && (w == null || a.world.equals(w))) {
                stop(a, false);
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ bucle

    public void startLoop() {
        task = Bukkit.getScheduler().runTaskTimer(pl, this::tick, 20L, 2L);
        nextAuto = System.currentTimeMillis() + randomGap();
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Active a : new ArrayList<>(actives)) {
            try {
                if (a.running) a.d.stop(a);
            } catch (Throwable ignored) {
            }
            if (a.bar != null) a.bar.removeAll();
            cleanup(a);
            if (a.scar) pl.ledger.forget(a);
            else pl.ledger.release(a, 0);
        }
        actives.clear();
    }

    private long randomGap() {
        int min = Math.max(1, pl.getConfig().getInt("auto.minimo-minutos", 45));
        int max = Math.max(min, pl.getConfig().getInt("auto.maximo-minutos", 120));
        return (min + rnd.nextInt(max - min + 1)) * 60_000L;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Active a : actives) {
            if (!a.running) {
                if (now >= a.warnUntil) activate(a);
                else if (a.bar != null) {
                    a.bar.setTitle(a.d.color + "⚠ " + a.d.name + " §7en §f" + ((a.warnUntil - now) / 1000 + 1) + " s");
                    a.bar.setProgress(Math.max(0, Math.min(1, 1.0 - (a.warnUntil - now) / 15000.0)));
                }
                continue;
            }
            if (now >= a.end) {
                stop(a, true);
                continue;
            }
            a.beat++;
            if (a.bar != null && a.beat % 5 == 0) {
                a.bar.setTitle(a.d.color + "§l" + a.d.name + " §7· " + Util.bars(a.level) + " §7· §f" + Util.time(a.msLeft()));
                a.bar.setProgress(Math.max(0, Math.min(1, 1.0 - a.progress())));
                if (a.beat % 50 == 0) {
                    a.bar.removeAll();
                    for (Player p : a.world.getPlayers()) a.bar.addPlayer(p);
                }
            }
            if (a.beat % Math.max(1, a.d.every()) == 0) {
                try {
                    a.d.tick(a);
                } catch (Throwable t) {
                    pl.getLogger().warning("Error en " + a.d.id + ": " + t + " — se detiene.");
                    t.printStackTrace();
                    stop(a, false);
                }
                if (a.beat == 60) checkRefusals(a);
            }
        }
        pl.ledger.tick();
        if (pl.getConfig().getBoolean("auto.activo", false) && now >= nextAuto) {
            if (pl.getConfig().getBoolean("auto.horario.activo", false)) {
                int h = java.time.LocalTime.now().getHour(), from = pl.getConfig().getInt("auto.horario.desde", 17), to = pl.getConfig().getInt("auto.horario.hasta", 23);
                boolean in = from <= to ? (h >= from && h < to) : (h >= from || h < to);
                if (!in) {
                    nextAuto = now + 60_000L;
                    return;
                }
            }
            nextAuto = now + randomGap();
            autoPick();
        }
    }

    /** Si un desastre de bloques no pudo cambiar nada, se lo dice a quien lo lanzó (y a la consola). */
    private void checkRefusals(Active a) {
        int[] st = pl.ledger.stat.get(a.id);
        if (st == null || st[0] > 0 || st[1] + st[2] == 0) return;
        String msg = st[2] > 0
                ? "§e⚠ " + a.d.name + " no puede cambiar bloques porque en config.yml tienes §fdestruccion: false§e."
                : "§e⚠ " + a.d.name + " no cambia bloques aquí: estás dentro de una zona protegida (spawn §f" + pl.getConfig().getInt("zona-spawn-protegida", 30)
                + " bloques§e o zonas-seguras). Aléjate del spawn o baja §fzona-spawn-protegida§e.";
        pl.getLogger().info(a.d.id + ": " + ChatColorStrip.strip(msg));
        Player p = Bukkit.getPlayerExact(a.by);
        if (p != null) p.sendMessage(msg);
        else for (Player o : a.world.getPlayers()) if (o.isOp()) o.sendMessage(msg);
    }

    /** Lanza un desastre aleatorio (según pesos) en un mundo con jugadores. */
    public String autoPick() {
        List<World> ws = new ArrayList<>();
        int minPl = pl.getConfig().getInt("auto.jugadores-minimos", 1);
        for (World w : worlds()) if (w.getPlayers().size() >= minPl) ws.add(w);
        if (ws.isEmpty()) return "No hay mundos con suficientes jugadores.";
        World w = ws.get(rnd.nextInt(ws.size()));
        List<Disaster> pool = new ArrayList<>();
        int total = 0;
        for (Disaster d : reg.values()) {
            if (!enabled(d) || !d.worldOk(w) || weight(d) <= 0) continue;
            pool.add(d);
            total += weight(d);
        }
        if (pool.isEmpty()) return "No hay desastres activados.";
        int r = rnd.nextInt(total);
        Disaster pick = pool.get(0);
        for (Disaster d : pool) {
            r -= weight(d);
            if (r < 0) {
                pick = d;
                break;
            }
        }
        int level = 1 + (rnd.nextInt(100) < 25 ? 1 : 0) + (rnd.nextInt(100) < 8 ? 1 : 0);
        return start(pick, w, minutes(pick), level, pl.getConfig().getInt("aviso-segundos", 15), "auto", false);
    }

    public void resetAutoTimer() {
        nextAuto = System.currentTimeMillis() + randomGap();
    }

    public long msToAuto() {
        return Math.max(0, nextAuto - System.currentTimeMillis());
    }
}
