package com.etheria.consecuencias;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public final class Util {
    private Util() {
    }

    public static boolean valid(Player p) {
        GameMode g = p.getGameMode();
        return (g == GameMode.SURVIVAL || g == GameMode.ADVENTURE) && !p.hasPermission("consecuencias.inmune");
    }

    public static void actionbar(Player p, String msg) {
        p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));
    }

    public static String strip(String s) {
        return s.replaceAll("§.", "");
    }

    public static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    public static String bars(int level) {
        return "§c" + "▮".repeat(level) + "§8" + "▯".repeat(3 - level);
    }

    /** Punto aleatorio en un anillo alrededor de c (misma altura). */
    public static Location ring(Location c, double min, double max, Random r) {
        double ang = r.nextDouble() * Math.PI * 2, d = min + r.nextDouble() * Math.max(0.01, max - min);
        return c.clone().add(Math.cos(ang) * d, 0, Math.sin(ang) * d);
    }

    public static boolean loaded(World w, int x, int z) {
        return w.isChunkLoaded(x >> 4, z >> 4);
    }

    /** Bloque más alto en esa columna (null si el chunk no está cargado). */
    public static Block top(World w, int x, int z) {
        if (!loaded(w, x, z)) return null;
        return w.getBlockAt(x, w.getHighestBlockYAt(x, z), z);
    }

    /** Aire libre encima del jugador. */
    public static boolean exposed(Player p) {
        Location l = p.getLocation();
        return l.getBlockY() >= p.getWorld().getHighestBlockYAt(l) - 1;
    }

    public static boolean underground(Player p) {
        Location l = p.getLocation();
        World w = p.getWorld();
        return l.getBlock().getLightFromSky() <= 2 && l.getBlockY() < w.getHighestBlockYAt(l) - 5;
    }

    public static boolean inWater(Player p) {
        Material m = p.getLocation().getBlock().getType();
        return m == Material.WATER || p.isInWater();
    }

    public static boolean passable(Block b) {
        return b.isPassable() && !b.isLiquid();
    }

    // ------------------------------------------------------------------ etiquetas de entidades

    private static NamespacedKey key() {
        return new NamespacedKey(ConsecuenciasPlugin.get(), "desastre");
    }

    public static void tag(Entity e, Active a) {
        e.getPersistentDataContainer().set(key(), PersistentDataType.STRING, a.d.id + ":" + a.id);
        e.setPersistent(false); // no se guardan al reiniciar
        a.mobs.add(e);
    }

    /** "id:numero" o null. */
    public static String tagOf(Entity e) {
        return e.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
    }

    public static boolean tagged(Entity e, String disasterId) {
        String t = tagOf(e);
        return t != null && t.startsWith(disasterId + ":");
    }

    public static int count(World w, Active a) {
        return a.mobCount();
    }

    public static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
