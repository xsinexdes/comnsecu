package com.etheria.consecuencias.disasters;

import com.etheria.consecuencias.Active;
import com.etheria.consecuencias.Announcer;
import com.etheria.consecuencias.Disaster;
import com.etheria.consecuencias.DisasterManager;
import com.etheria.consecuencias.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Bendiciones: eventos buenos que dan vida al mundo. */
public final class BlessingDisasters {
    private BlessingDisasters() {
    }

    public static void register(DisasterManager m) {
        m.register(estrellas());
        m.register(cosecha());
        m.register(aurora());
    }

    // ------------------------------------------------------------------ Lluvia de estrellas
    private static Disaster estrellas() {
        return new Disaster("lluvia_estrellas", "Lluvia de estrellas", "Bendición", Material.NETHER_STAR, 5, "§e",
                "Estrellas fugaces caen sin dañar a nadie y dejan polvo estelar, experiencia y algún tesoro.") {
            @Override
            public String warning() {
                return "El cielo se llena de estrellas fugaces. Salid a recoger lo que caiga.";
            }

            @Override
            public int every() {
                return 1;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                List<double[]> stars = (List<double[]>) a.data.computeIfAbsent("estrellas", k -> new ArrayList<double[]>());
                for (Iterator<double[]> it = stars.iterator(); it.hasNext(); ) {
                    double[] s = it.next(); // x, y, z, vx, vy, vz, groundY
                    s[0] += s[3];
                    s[1] += s[4];
                    s[2] += s[5];
                    Location l = new Location(a.world, s[0], s[1], s[2]);
                    a.world.spawnParticle(Particle.END_ROD, l, 4, 0.1, 0.1, 0.1, 0.02);
                    a.world.spawnParticle(Particle.FIREWORK, l, 2, 0.15, 0.15, 0.15, 0.01);
                    if (s[1] <= s[6]) {
                        land(a, l);
                        it.remove();
                    }
                }
                if (a.beat % 10 != 0) return;
                for (Player p : a.world.getPlayers()) {
                    if (stars.size() > 10 + 5 * a.level || a.rnd.nextInt(100) >= 25 + 15 * a.level) continue;
                    Location t = Util.ring(p.getLocation(), 8, 45, a.rnd);
                    Block tb = Util.top(a.world, t.getBlockX(), t.getBlockZ());
                    if (tb == null) continue;
                    double gy = tb.getY() + 1;
                    double ox = (a.rnd.nextDouble() - 0.5) * 30, oz = (a.rnd.nextDouble() - 0.5) * 30;
                    double h = 70;
                    stars.add(new double[]{t.getX() + ox, gy + h, t.getZ() + oz, -ox / 40.0, -h / 40.0, -oz / 40.0, gy});
                }
            }

            private void land(Active a, Location l) {
                a.world.spawnParticle(Particle.FIREWORK, l, 40, 0.8, 0.5, 0.8, 0.15);
                a.world.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 3f, 1.2f);
                a.world.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 3f, 1.0f);
                ExperienceOrb orb = a.world.spawn(l.clone().add(0, 0.3, 0), ExperienceOrb.class);
                orb.setExperience(4 + 3 * a.level);
                var list = D.pl().getConfig().getStringList("desastres.lluvia_estrellas.botin");
                if (!list.isEmpty() && a.rnd.nextInt(100) < 55) {
                    String[] p = list.get(a.rnd.nextInt(list.size())).split(":");
                    Material mat = Material.matchMaterial(p[0]);
                    if (mat != null) {
                        int min = p.length > 1 ? Integer.parseInt(p[1].trim()) : 1, max = p.length > 2 ? Integer.parseInt(p[2].trim()) : min;
                        var it = a.world.dropItemNaturally(l.clone().add(0, 0.5, 0), new ItemStack(mat, min + a.rnd.nextInt(Math.max(1, max - min + 1))));
                        it.setGlowing(true);
                    }
                }
                if (a.rnd.nextInt(100) < 25) Announcer.news(a, "✦ Una estrella cayó cerca de §f" + l.getBlockX() + ", " + l.getBlockZ() + "§7.");
            }
        };
    }

    // ------------------------------------------------------------------ Cosecha abundante
    private static Disaster cosecha() {
        return new Disaster("cosecha_abundante", "Cosecha abundante", "Bendición", Material.GOLDEN_HOE, 8, "§a",
                "Los cultivos crecen a toda velocidad y los animales se emparejan solos.") {
            @Override
            public String warning() {
                return "La tierra se muestra generosa: los campos madurarán muy rápido.";
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.world.getPlayers()) {
                    int grown = 0;
                    for (int i = 0; i < 60 && grown < 6 * a.level; i++) {
                        Block b = p.getLocation().getBlock().getRelative(a.rnd.nextInt(29) - 14, a.rnd.nextInt(5) - 2, a.rnd.nextInt(29) - 14);
                        if (!Util.loaded(a.world, b.getX(), b.getZ())) continue;
                        if (b.getBlockData() instanceof Ageable ag && ag.getAge() < ag.getMaximumAge()) {
                            ag.setAge(Math.min(ag.getMaximumAge(), ag.getAge() + 1 + a.rnd.nextInt(2)));
                            b.setBlockData(ag, true);
                            a.world.spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.6, 0.5), 4, 0.3, 0.3, 0.3, 0);
                            grown++;
                        }
                    }
                    if (a.beat % 100 == 0) {
                        int n = 0;
                        for (Entity e : p.getNearbyEntities(24, 12, 24)) {
                            if (e instanceof Animals an && n < 6 && an.isAdult() && an.canBreed() && an.getLoveModeTicks() <= 0) {
                                an.setLoveModeTicks(600);
                                n++;
                            }
                        }
                        if (Util.valid(p)) Util.actionbar(p, "§a🌾 Cosecha abundante: §7los campos florecen");
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Aurora boreal
    private static Disaster aurora() {
        return new Disaster("aurora", "Aurora boreal", "Bendición", Material.AMETHYST_SHARD, 6, "§d",
                "Noche mágica con luces en el cielo: regeneración, velocidad y orbes de experiencia.") {
            @Override
            public String warning() {
                return "Una aurora cubrirá el cielo esta noche. Sus luces curan y dan energía.";
            }

            @Override
            public List<String> conflicts() {
                return List.of("eclipse", "luna_sangre");
            }

            @Override
            public void start(Active a) {
                D.night(a);
            }

            @Override
            public int every() {
                return 4;
            }

            @Override
            public void tick(Active a) {
                D.keepNight(a);
                Color[] cols = {Color.fromRGB(80, 255, 150), Color.fromRGB(120, 140, 255), Color.fromRGB(200, 100, 255)};
                for (Player p : a.world.getPlayers()) {
                    Location base = p.getLocation().add(0, 38, 0);
                    for (int i = 0; i < 18; i++) {
                        double x = (i - 9) * 5 + Math.sin(a.beat * 0.05 + i * 0.4) * 4, y = Math.sin(a.beat * 0.03 + i * 0.7) * 5;
                        for (int k = 0; k < 3; k++) {
                            a.world.spawnParticle(Particle.DUST, base.clone().add(x, y + k * 2, -30 + Math.cos(i * 0.3) * 6), 1, 0.2, 0.4, 0.2, new Particle.DustOptions(cols[(i + k) % 3], 3.0f));
                        }
                    }
                    if (!Util.valid(p)) continue;
                    if (a.beat % 20 == 0) {
                        D.eff(p, PotionEffectType.REGENERATION, 80, 0);
                        D.eff(p, PotionEffectType.SPEED, 80, 0);
                        Util.actionbar(p, "§d✦ La aurora te llena de energía");
                    }
                    if (a.beat % 200 == 0) {
                        ExperienceOrb orb = a.world.spawn(p.getLocation().add(a.rnd.nextInt(5) - 2, 1, a.rnd.nextInt(5) - 2), ExperienceOrb.class);
                        orb.setExperience(3 + 2 * a.level);
                    }
                }
            }

            @Override
            public void stop(Active a) {
                D.endNight(a);
            }
        };
    }
}
