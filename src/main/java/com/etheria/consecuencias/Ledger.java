package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Libro de cambios: todo bloque que un desastre modifica se apunta aquí con su estado original,
 * y se restaura al terminar (poco a poco). Se guarda en disco: si el servidor se cae, al arrancar se repara.
 */
public class Ledger {
    private static final class Saved {
        String world;
        int x, y, z;
        String data;
        int owner;
        long restoreAt = Long.MAX_VALUE;
    }

    private final ConsecuenciasPlugin plugin;
    private final File file;
    private final Map<String, Saved> map = new LinkedHashMap<>();
    private final Deque<Saved> queue = new ArrayDeque<>();
    /** Por desastre: {cambios hechos, rechazados por zona segura, rechazados por destruccion:false}. */
    public final Map<Integer, int[]> stat = new java.util.HashMap<>();
    private boolean dirty;
    private long lastSave;

    public Ledger(ConsecuenciasPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ledger.yml");
        load();
    }

    private static String k(String w, int x, int y, int z) {
        return w + ";" + x + ";" + y + ";" + z;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String line : y.getStringList("bloques")) {
            String[] p = line.split(";", 5);
            if (p.length < 5) continue;
            try {
                Saved s = new Saved();
                s.world = p[0];
                s.x = Integer.parseInt(p[1]);
                s.y = Integer.parseInt(p[2]);
                s.z = Integer.parseInt(p[3]);
                s.data = p[4];
                s.owner = 0;
                s.restoreAt = 0; // lo que quedó de una sesión anterior se repara ya
                map.put(k(s.world, s.x, s.y, s.z), s);
                queue.add(s);
            } catch (NumberFormatException ignored) {
            }
        }
        if (!map.isEmpty()) plugin.getLogger().info("Ledger: " + map.size() + " bloques pendientes de restaurar de la sesión anterior.");
    }

    public int size() {
        return map.size();
    }

    public boolean full() {
        return map.size() >= plugin.getConfig().getInt("limite-bloques", 60000);
    }

    /** ¿Está el bloque en una zona segura (spawn o zonas definidas)? */
    public boolean safe(Block b) {
        World w = b.getWorld();
        int r = plugin.getConfig().getInt("zona-spawn-protegida", 30);
        if (r > 0) {
            Location s = w.getSpawnLocation();
            double dx = b.getX() - s.getX(), dz = b.getZ() - s.getZ();
            if (dx * dx + dz * dz <= (double) r * r) return true;
        }
        for (Map<?, ?> m : plugin.getConfig().getMapList("zonas-seguras")) {
            Object mw = m.get("mundo");
            if (mw != null && !w.getName().equalsIgnoreCase(String.valueOf(mw))) continue;
            double x = num(m.get("x")), z = num(m.get("z")), rad = num(m.get("radio"));
            double dx = b.getX() - x, dz = b.getZ() - z;
            if (dx * dx + dz * dz <= rad * rad) return true;
        }
        return false;
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    /** Apunta el estado actual del bloque (si aún no estaba apuntado). false si no se puede tocar. */
    public boolean record(Block b, Active owner) {
        int[] st = owner == null ? null : stat.computeIfAbsent(owner.id, x -> new int[3]);
        if (!plugin.getConfig().getBoolean("destruccion", true)) {
            if (st != null) st[2]++;
            return false;
        }
        if (b.getY() <= b.getWorld().getMinHeight()) return false;
        if (safe(b)) {
            if (st != null) st[1]++;
            return false;
        }
        String key = k(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        if (st != null) st[0]++;
        if (map.containsKey(key)) return true;
        if (full()) return false;
        Saved s = new Saved();
        s.world = b.getWorld().getName();
        s.x = b.getX();
        s.y = b.getY();
        s.z = b.getZ();
        s.data = b.getBlockData().getAsString();
        s.owner = owner == null ? 0 : owner.id;
        map.put(key, s);
        dirty = true;
        return true;
    }

    /** Cambia un bloque dejando constancia para restaurarlo. */
    public boolean set(Block b, Material m, Active owner) {
        if (b.getType() == m) return false;
        if (b.getType() == Material.BEDROCK || b.getType() == Material.END_PORTAL_FRAME || b.getType() == Material.SPAWNER) return false;
        if (!record(b, owner)) return false;
        b.setType(m, false);
        return true;
    }

    public boolean set(Block b, BlockData d, Active owner) {
        if (!record(b, owner)) return false;
        b.setBlockData(d, false);
        return true;
    }

    /** Marca los bloques de un desastre para restaurarlos dentro de 'delayMs'. */
    public void release(Active a, long delayMs) {
        long at = System.currentTimeMillis() + delayMs;
        boolean regen = plugin.getConfig().getBoolean("regenerar", true);
        for (Saved s : new ArrayList<>(map.values())) {
            if (s.owner != a.id || s.restoreAt != Long.MAX_VALUE) continue;
            if (regen) {
                s.restoreAt = at;
                queue.add(s);
            } else {
                map.remove(k(s.world, s.x, s.y, s.z));
            }
        }
        dirty = true;
    }

    /** Cicatriz: olvida los bloques de este desastre (quedan cambiados para siempre). */
    public int forget(Active a) {
        int n = 0;
        for (Saved s : new ArrayList<>(map.values())) {
            if (s.owner != a.id || s.restoreAt != Long.MAX_VALUE) continue;
            map.remove(k(s.world, s.x, s.y, s.z));
            n++;
        }
        dirty = true;
        return n;
    }

    /** Restaura un lote de bloques cuyo momento ya llegó. */
    public void tick() {
        if (queue.isEmpty()) {
            if (dirty && System.currentTimeMillis() - lastSave > 30_000) save(false);
            return;
        }
        long now = System.currentTimeMillis();
        int budget = plugin.getConfig().getInt("regenerar-velocidad", 120);
        int guard = budget * 3;
        while (budget > 0 && guard-- > 0 && !queue.isEmpty()) {
            Saved s = queue.peekFirst();
            if (s.restoreAt > now) break;
            queue.pollFirst();
            String key = k(s.world, s.x, s.y, s.z);
            if (map.get(key) != s) continue; // ya restaurado por otra vía
            World w = Bukkit.getWorld(s.world);
            if (w == null) {
                map.remove(key);
                dirty = true;
                continue;
            }
            if (!w.isChunkLoaded(s.x >> 4, s.z >> 4)) {
                queue.addLast(s); // se restaura cuando el chunk se cargue
                continue;
            }
            try {
                w.getBlockAt(s.x, s.y, s.z).setBlockData(Bukkit.createBlockData(s.data), false);
            } catch (Exception ignored) {
            }
            map.remove(key);
            dirty = true;
            budget--;
        }
        if (dirty && now - lastSave > 30_000) save(false);
    }

    /** Restaura un bloque concreto ahora mismo (si estaba apuntado). */
    public void revert(Block b) {
        Saved s = map.remove(k(b.getWorld().getName(), b.getX(), b.getY(), b.getZ()));
        if (s == null) return;
        try {
            b.setBlockData(Bukkit.createBlockData(s.data), false);
        } catch (Exception ignored) {
        }
        dirty = true;
    }

    /** Restaura todo ya (al parar el servidor o con /desastre reparar). */
    public int restoreNow() {
        int n = 0;
        for (Saved s : new ArrayList<>(map.values())) {
            World w = Bukkit.getWorld(s.world);
            if (w == null) continue;
            try {
                w.getChunkAt(s.x >> 4, s.z >> 4).load();
                w.getBlockAt(s.x, s.y, s.z).setBlockData(Bukkit.createBlockData(s.data), false);
                map.remove(k(s.world, s.x, s.y, s.z));
                n++;
            } catch (Exception ignored) {
            }
        }
        queue.clear();
        dirty = true;
        save(true);
        return n;
    }

    public void save(boolean sync) {
        lastSave = System.currentTimeMillis();
        dirty = false;
        final List<String> lines = new ArrayList<>(map.size());
        for (Saved s : map.values()) lines.add(s.world + ";" + s.x + ";" + s.y + ";" + s.z + ";" + s.data);
        Runnable w = () -> {
            YamlConfiguration y = new YamlConfiguration();
            y.set("bloques", lines);
            try {
                y.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo guardar ledger.yml: " + e.getMessage());
            }
        };
        if (sync || !plugin.isEnabled()) w.run();
        else Bukkit.getScheduler().runTaskAsynchronously(plugin, w);
    }
}
