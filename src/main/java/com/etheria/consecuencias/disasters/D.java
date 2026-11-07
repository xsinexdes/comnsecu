package com.etheria.consecuencias.disasters;

import com.etheria.consecuencias.Active;
import com.etheria.consecuencias.ConsecuenciasPlugin;
import com.etheria.consecuencias.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Tag;

import java.util.function.Consumer;

/** Utilidades comunes a los desastres. */
final class D {
    private D() {
    }

    static ConsecuenciasPlugin pl() {
        return ConsecuenciasPlugin.get();
    }

    static void eff(Player p, PotionEffectType t, int ticks, int amp) {
        p.addPotionEffect(new PotionEffect(t, ticks, amp, false, false, true));
    }

    /** Llama a fn con el bloque más alto de n columnas al azar alrededor del jugador. */
    static void columns(Active a, Player p, int n, double radius, Consumer<Block> fn) {
        Location c = p.getLocation();
        for (int i = 0; i < n; i++) {
            double ang = a.rnd.nextDouble() * Math.PI * 2, d = 3 + a.rnd.nextDouble() * Math.max(1, radius - 3);
            int x = (int) Math.floor(c.getX() + Math.cos(ang) * d), z = (int) Math.floor(c.getZ() + Math.sin(ang) * d);
            Block b = Util.top(a.world, x, z);
            if (b != null) fn.accept(b);
        }
    }

    static boolean soft(Material m) {
        return Tag.LEAVES.isTagged(m) || Tag.FLOWERS.isTagged(m) || Tag.CROPS.isTagged(m)
                || m == Material.SHORT_GRASS || m == Material.TALL_GRASS || m == Material.FERN || m == Material.LARGE_FERN
                || m == Material.SWEET_BERRY_BUSH || m == Material.VINE || m == Material.SNOW || Tag.WOOL.isTagged(m);
    }

    /** Nivel base de agua para un jugador (suelo bajo sus pies cuando empezó el desastre). */
    static int baseY(Active a, Player p) {
        String k = "base:" + p.getUniqueId();
        Object o = a.data.get(k);
        if (o instanceof Integer i) return i;
        Block t = Util.top(a.world, p.getLocation().getBlockX(), p.getLocation().getBlockZ());
        int y = t == null ? p.getLocation().getBlockY() - 1 : t.getY();
        a.data.put(k, y);
        return y;
    }

    /** Inunda columnas alrededor del jugador hasta floodY (agua estática, queda apuntada en el ledger). Devuelve cuántos bloques de agua puso. */
    static int flood(Active a, Player p, int floodY, int n, double radius) {
        int[] placed = {0};
        columns(a, p, n, radius, top -> {
            int start = top.getY() + 1;
            if (!top.isLiquid() && !top.getType().isSolid() && !Tag.LEAVES.isTagged(top.getType())) start = top.getY();
            if (start > floodY) return;
            int cap = Math.min(floodY, start + 12);
            for (int y = start; y <= cap; y++) {
                Block b = a.world.getBlockAt(top.getX(), y, top.getZ());
                if (b.getType() == Material.WATER) continue;
                if (b.getType().isAir() || soft(b.getType())) {
                    if (pl().ledger.set(b, Material.WATER, a)) placed[0]++;
                    else return;
                } else break;
            }
            if (a.rnd.nextInt(4) == 0) a.world.spawnParticle(org.bukkit.Particle.SPLASH, top.getLocation().add(0.5, Math.max(1, cap - top.getY()), 0.5), 12, 0.4, 0.1, 0.4, 0.1);
        });
        return placed[0];
    }

    /** Anillo de partículas sobre el suelo. */
    static void ring(World w, Location c, double r, Particle part, int pts) {
        for (int k = 0; k < pts; k++) {
            double ang = Math.PI * 2 * k / Math.max(1, pts);
            w.spawnParticle(part, c.getX() + Math.cos(ang) * r, c.getY() + 0.3, c.getZ() + Math.sin(ang) * r, 1, 0, 0.1, 0, 0.02);
        }
    }

    /** Onda expansiva visual (anillos que se agrandan). */
    static void shockwave(World w, Location c, double maxR, Particle part) {
        for (int i = 1; i <= 10; i++) {
            final double r = maxR * i / 10.0;
            Bukkit.getScheduler().runTaskLater(pl(), () -> ring(w, c, r, part, (int) Math.max(8, r * 6)), i * 2L);
        }
    }

    /** Sacudida: empuja suavemente a los jugadores cercanos (sin dañar bloques). */
    static void shake(World w, Location c, double radius, double power) {
        for (Player p : w.getPlayers()) {
            if (!Util.valid(p) || p.getLocation().distanceSquared(c) > radius * radius) continue;
            p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector((Math.random() - 0.5) * power, 0.18 * power * 2, (Math.random() - 0.5) * power)));
        }
    }

    static String compass(double dx, double dz) {
        double ang = Math.toDegrees(Math.atan2(dz, dx)); // 0 = este, 90 = sur
        String[] n = {"Este", "Sureste", "Sur", "Suroeste", "Oeste", "Noroeste", "Norte", "Noreste"};
        return n[(int) Math.floorMod(Math.round(ang / 45.0), 8L)];
    }

    static void lightning(World w, Location l, boolean real) {
        if (real && pl().getConfig().getBoolean("destruccion", true)) w.strikeLightning(l);
        else w.strikeLightningEffect(l);
    }

    static void night(Active a) {
        a.data.put("ciclo", a.world.getGameRuleValue(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE));
        a.data.put("hora", a.world.getTime());
        a.world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        a.world.setTime(18000);
    }

    static void keepNight(Active a) {
        if (a.world.getTime() < 13000 || a.world.getTime() > 23000) a.world.setTime(18000);
    }

    static void endNight(Active a) {
        Object c = a.data.get("ciclo");
        a.world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, c instanceof Boolean b ? b : Boolean.TRUE);
        a.world.setTime(1000);
    }

    /** Número de entidades etiquetadas por este desastre (para respetar límites). */
    static int mobs(Active a) {
        return Util.count(a.world, a);
    }
}
