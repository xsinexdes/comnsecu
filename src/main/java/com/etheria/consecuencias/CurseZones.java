package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Zonas malditas permanentes (p. ej. una cueva con veneno, un páramo ardiente). Las crea el admin con
 * /desastre maldita crear <tipo> <radio> [subterranea]. Se guardan en zonas-malditas.yml.
 */
public class CurseZones {
    public static final List<String> TYPES = List.of("veneno", "fuego", "hielo", "oscuridad", "radiacion", "pantano");

    public static final class Zone {
        public String world, type;
        public int x, y, z, radius;
        public boolean underground;
    }

    private final ConsecuenciasPlugin pl;
    private final File file;
    private final List<Zone> zones = new ArrayList<>();
    private int beat;

    public CurseZones(ConsecuenciasPlugin pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "zonas-malditas.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(pl, this::tick, 40L, 10L);
    }

    public List<Zone> list() {
        return zones;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String line : y.getStringList("zonas")) {
            String[] p = line.split(";");
            if (p.length < 7) continue;
            try {
                Zone z = new Zone();
                z.world = p[0];
                z.type = p[1];
                z.x = Integer.parseInt(p[2]);
                z.y = Integer.parseInt(p[3]);
                z.z = Integer.parseInt(p[4]);
                z.radius = Integer.parseInt(p[5]);
                z.underground = Boolean.parseBoolean(p[6]);
                zones.add(z);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void save() {
        List<String> lines = new ArrayList<>();
        for (Zone z : zones) lines.add(z.world + ";" + z.type + ";" + z.x + ";" + z.y + ";" + z.z + ";" + z.radius + ";" + z.underground);
        YamlConfiguration y = new YamlConfiguration();
        y.set("zonas", lines);
        try {
            y.save(file);
        } catch (IOException e) {
            pl.getLogger().warning("No se pudo guardar zonas-malditas.yml: " + e.getMessage());
        }
    }

    public Zone add(Location l, String type, int radius, boolean underground) {
        Zone z = new Zone();
        z.world = l.getWorld().getName();
        z.type = type;
        z.x = l.getBlockX();
        z.y = l.getBlockY();
        z.z = l.getBlockZ();
        z.radius = Math.max(4, Math.min(300, radius));
        z.underground = underground;
        zones.add(z);
        save();
        return z;
    }

    public boolean remove(int i) {
        if (i < 0 || i >= zones.size()) return false;
        zones.remove(i);
        save();
        return true;
    }

    private void tick() {
        if (zones.isEmpty()) return;
        beat++;
        for (Zone z : zones) {
            World w = Bukkit.getWorld(z.world);
            if (w == null || w.getPlayers().isEmpty()) continue;
            double r2 = (double) z.radius * z.radius;
            for (Player p : w.getPlayers()) {
                Location l = p.getLocation();
                double dx = l.getX() - z.x, dz = l.getZ() - z.z;
                double d2 = dx * dx + dz * dz;
                // borde visible para quien se acerca
                if (d2 < (z.radius + 24.0) * (z.radius + 24.0) && beat % 2 == 0) edge(w, p, z);
                if (d2 > r2) continue;
                if (z.underground && !Util.underground(p)) continue;
                if (!Util.valid(p)) continue;
                apply(p, z, w);
            }
        }
    }

    private void edge(World w, Player p, Zone z) {
        double ang = Math.atan2(p.getLocation().getZ() - z.z, p.getLocation().getX() - z.x);
        for (int i = -4; i <= 4; i++) {
            double a = ang + i * 0.06;
            double px = z.x + Math.cos(a) * z.radius, pz = z.z + Math.sin(a) * z.radius;
            Location l = new Location(w, px, p.getLocation().getY() + 1, pz);
            switch (z.type) {
                case "veneno" -> p.spawnParticle(Particle.DUST, l, 2, 0.2, 1.5, 0.2, 0, new Particle.DustOptions(Color.fromRGB(80, 200, 60), 1.6f));
                case "fuego" -> p.spawnParticle(Particle.FLAME, l, 2, 0.2, 1.5, 0.2, 0.01);
                case "hielo" -> p.spawnParticle(Particle.SNOWFLAKE, l, 3, 0.2, 1.5, 0.2, 0.01);
                case "oscuridad" -> p.spawnParticle(Particle.SQUID_INK, l, 2, 0.2, 1.5, 0.2, 0.01);
                case "radiacion" -> p.spawnParticle(Particle.DUST, l, 2, 0.2, 1.5, 0.2, 0, new Particle.DustOptions(Color.fromRGB(230, 255, 40), 1.6f));
                default -> p.spawnParticle(Particle.SPORE_BLOSSOM_AIR, l, 3, 0.2, 1.5, 0.2, 0);
            }
        }
    }

    private void apply(Player p, Zone z, World w) {
        Location l = p.getLocation().add(0, 1, 0);
        boolean sec = beat % 2 == 0; // cada segundo
        switch (z.type) {
            case "veneno" -> {
                w.spawnParticle(Particle.DUST, l, 6, 2, 1, 2, 0, new Particle.DustOptions(Color.fromRGB(80, 200, 60), 1.4f));
                if (sec && !Gear.airSafe(p)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0, false, true, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, false, true));
                    Util.actionbar(p, "§2☣ Aire tóxico §7— necesitas una máscara antigás");
                    if (Gear.hasMask(p)) Gear.wearMask(p, 1);
                } else if (sec && Gear.hasMask(p)) Gear.wearMask(p, 1);
            }
            case "fuego" -> {
                w.spawnParticle(Particle.FLAME, l, 6, 2, 1, 2, 0.01);
                if (sec && !p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                    p.setFireTicks(Math.max(p.getFireTicks(), 60));
                    Util.actionbar(p, "§6🔥 Suelo ardiente §7— resistencia al fuego");
                }
            }
            case "hielo" -> {
                w.spawnParticle(Particle.SNOWFLAKE, l, 10, 2, 1, 2, 0.01);
                if (sec && !Gear.hasCoat(p)) {
                    p.setFreezeTicks(Math.min(p.getMaxFreezeTicks(), p.getFreezeTicks() + 50));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false, true));
                    Util.actionbar(p, "§b❄ Frío mortal §7— necesitas un abrigo térmico");
                }
            }
            case "oscuridad" -> {
                w.spawnParticle(Particle.SQUID_INK, l, 6, 2, 1, 2, 0.01);
                if (sec) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, false, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, false, false, true));
                }
            }
            case "radiacion" -> {
                w.spawnParticle(Particle.DUST, l, 6, 2, 1, 2, 0, new Particle.DustOptions(Color.fromRGB(230, 255, 40), 1.4f));
                if (sec && !Gear.airSafe(p)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0, false, true, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1, false, false, true));
                    Util.actionbar(p, "§e☢ Radiación §7— máscara o antídoto");
                }
            }
            default -> { // pantano
                w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, l, 8, 2, 1, 2, 0);
                if (sec) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 0, false, false, true));
                    Util.actionbar(p, "§a🐸 Pantano maldito §7— el barro te frena");
                }
            }
        }
    }
}
