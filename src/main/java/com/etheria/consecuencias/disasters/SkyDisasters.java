package com.etheria.consecuencias.disasters;

import com.etheria.consecuencias.Active;
import com.etheria.consecuencias.Disaster;
import com.etheria.consecuencias.Gear;
import com.etheria.consecuencias.Announcer;
import com.etheria.consecuencias.DisasterManager;
import com.etheria.consecuencias.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Cielo: meteoritos, lluvia de fuego, eclipse, luna de sangre, gravedad lunar. */
public final class SkyDisasters {
    private SkyDisasters() {
    }

    public static void register(DisasterManager m) {
        m.register(meteoritos());
        m.register(lluviaFuego());
        m.register(eclipse());
        m.register(lunaSangre());
        m.register(gravedad());
        m.register(magnetica());
        m.register(fantasmas());
    }

    // ------------------------------------------------------------------ Meteoritos
    private static Disaster meteoritos() {
        return new Disaster("meteoritos", "Lluvia de meteoritos", "Cielo", Material.FIRE_CHARGE, 6, "§d",
                "Rocas ardientes caen del cielo. Dejan cráteres y minerales raros.") {
            @Override
            public String warning() {
                return "El cielo se rompe. Cada impacto deja botín... y destrucción.";
            }

            @Override
            public int every() {
                return 1;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                List<Fireball> fbs = (List<Fireball>) a.data.computeIfAbsent("fb", k -> new ArrayList<Fireball>());
                fbs.removeIf(f -> f.isDead() || !f.isValid());
                java.util.Map<java.util.UUID, Location> tg = (java.util.Map<java.util.UUID, Location>) a.data.computeIfAbsent("tg", k -> new java.util.HashMap<java.util.UUID, Location>());
                for (Fireball f : fbs) {
                    Location l = f.getLocation();
                    a.world.spawnParticle(Particle.FLAME, l, 14, 0.7, 0.7, 0.7, 0.04);
                    a.world.spawnParticle(Particle.LARGE_SMOKE, l, 8, 0.6, 0.6, 0.6, 0.02);
                    a.world.spawnParticle(Particle.LAVA, l, 3, 0.4, 0.4, 0.4, 0);
                    a.world.spawnParticle(Particle.FIREWORK, l, 4, 0.2, 0.2, 0.2, 0.08);
                    // estela larga detrás del meteorito
                    Vector back = f.getVelocity().clone().normalize().multiply(-1);
                    for (int i = 1; i <= 6; i++) a.world.spawnParticle(Particle.SMOKE, l.clone().add(back.clone().multiply(i * 1.4)), 2, 0.2, 0.2, 0.2, 0.01);
                    Location t = tg.get(f.getUniqueId());
                    if (t != null) {
                        if (a.beat % 4 == 0) {
                            double pulse = 2.5 + (a.beat % 8) * 0.12;
                            for (int k = 0; k < 20; k++) {
                                double ang = Math.PI * 2 * k / 20;
                                a.world.spawnParticle(Particle.DUST, t.getX() + Math.cos(ang) * pulse, t.getY() + 0.2, t.getZ() + Math.sin(ang) * pulse, 1, 0, 0, 0, 0, new Particle.DustOptions(org.bukkit.Color.RED, 1.6f));
                            }
                            a.world.spawnParticle(Particle.DUST, t.clone().add(0, 0.3, 0), 3, 0.2, 0.1, 0.2, 0, new Particle.DustOptions(org.bukkit.Color.ORANGE, 2f));
                        }
                        if (a.beat % 10 == 0) for (Player p : a.players()) {
                            double d = p.getLocation().distance(t);
                            if (d < 30) {
                                Util.actionbar(p, "§c☄ ¡IMPACTO INMINENTE! §7a §f" + (int) d + " §7bloques");
                                p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 0.5f);
                            }
                        }
                    }
                }
                tg.keySet().removeIf(u -> fbs.stream().noneMatch(f -> f.getUniqueId().equals(u)));
                if (a.beat % 10 != 0) return;
                int chance = 25 + 18 * a.level;
                for (Player p : a.players()) {
                    if (fbs.size() > 8 + 4 * a.level || a.rnd.nextInt(100) >= chance) continue;
                    Location target = Util.ring(p.getLocation(), 12, 55, a.rnd);
                    Block tb = Util.top(a.world, target.getBlockX(), target.getBlockZ());
                    if (tb == null) continue;
                    Location tl = tb.getLocation().add(0.5, 1, 0.5);
                    Location from = tl.clone().add(a.rnd.nextInt(41) - 20, 80, a.rnd.nextInt(41) - 20);
                    Vector dir = tl.toVector().subtract(from.toVector()).normalize();
                    boolean grief = D.pl().getConfig().getBoolean("destruccion", true);
                    LargeFireball f = a.world.spawn(from, LargeFireball.class, fb -> {
                        fb.setYield(2.2f + a.level * 0.9f);
                        fb.setIsIncendiary(grief);
                        fb.setDirection(dir);
                        fb.setVelocity(dir.clone().multiply(1.6));
                        fb.setGlowing(true);
                        Util.tag(fb, a);
                    });
                    fbs.add(f);
                    tg.put(f.getUniqueId(), tl.clone());
                    a.world.spawnParticle(Particle.FLASH, from, 1);
                    a.world.playSound(tl, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 4f, 0.4f);
                    a.world.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.3f);
                }
            }
        };
    }

    /** Se llama justo después de que un meteorito explote: efectos, botín y aviso. */
    public static void impact(Active a, Location l) {
        a.world.spawnParticle(Particle.FLASH, l.clone().add(0, 2, 0), 1);
        a.world.spawnParticle(Particle.BLOCK, l, 90, 3, 1, 3, 0.25, l.clone().subtract(0, 1, 0).getBlock().getBlockData());
        a.world.spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 2, 0), 40, 2.5, 3, 2.5, 0.05);
        D.shockwave(a.world, l, 9 + 3 * a.level, Particle.CLOUD);
        D.shockwave(a.world, l, 6 + 2 * a.level, Particle.FLAME);
        D.shake(a.world, l, 30, 0.5);
        a.world.strikeLightningEffect(l);
        a.world.spawnParticle(Particle.EXPLOSION_EMITTER, l, 2, 1, 1, 1, 0);
        a.world.spawnParticle(Particle.LAVA, l, 40, 3, 1, 3, 0);
        a.world.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 6f, 0.6f);
        var list = D.pl().getConfig().getStringList("desastres.meteoritos.botin");
        int total = 0;
        List<String[]> opts = new ArrayList<>();
        for (String s : list) {
            String[] p = s.split(":");
            if (p.length < 4 || Material.matchMaterial(p[0]) == null) continue;
            opts.add(p);
            total += Integer.parseInt(p[3].trim());
        }
        if (total > 0) {
            int rolls = 2 + a.level;
            for (int i = 0; i < rolls; i++) {
                int r = a.rnd.nextInt(total);
                for (String[] p : opts) {
                    r -= Integer.parseInt(p[3].trim());
                    if (r < 0) {
                        int min = Integer.parseInt(p[1].trim()), max = Integer.parseInt(p[2].trim());
                        Item it = a.world.dropItemNaturally(l.clone().add(0, 1, 0), new ItemStack(Material.matchMaterial(p[0]), min + a.rnd.nextInt(Math.max(1, max - min + 1))));
                        it.setGlowing(true);
                        break;
                    }
                }
            }
        }
        if (a.rnd.nextInt(100) < 60) Announcer.news(a, "☄ Impacto de meteorito cerca de §f" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + "§7. ¡Hay minerales!");
    }

    // ------------------------------------------------------------------ Lluvia de fuego
    private static Disaster lluviaFuego() {
        return new Disaster("lluvia_fuego", "Lluvia de fuego", "Cielo", Material.BLAZE_ROD, 4, "§c",
                "Bolas de fuego pequeñas llueven sobre los jugadores.") {
            @Override
            public List<String> conflicts() {
                return List.of("diluvio", "inundacion", "ventisca");
            }

            @Override
            public int every() {
                return 4;
            }

            @Override
            public void tick(Active a) {
                boolean grief = D.pl().getConfig().getBoolean("destruccion", true);
                for (Player p : a.world.getPlayers()) {
                    for (int i = 0; i < a.level; i++) {
                        if (a.rnd.nextInt(100) > 70) continue;
                        Location from = p.getLocation().add(a.rnd.nextInt(41) - 20, 26, a.rnd.nextInt(41) - 20);
                        Vector dir = new Vector((a.rnd.nextDouble() - 0.5) * 0.2, -1, (a.rnd.nextDouble() - 0.5) * 0.2).normalize();
                        a.world.spawn(from, SmallFireball.class, f -> {
                            f.setIsIncendiary(grief);
                            f.setDirection(dir);
                            f.setVelocity(dir.clone().multiply(0.9));
                            Util.tag(f, a);
                        });
                    }
                    if (Util.valid(p) && a.beat % 40 == 0) Util.actionbar(p, "§c🔥 ¡Llueve fuego! §7Busca techo");
                }
            }
        };
    }

    // ------------------------------------------------------------------ hordas nocturnas
    private static void horde(Active a, int perLevelCap, int chance) {
        for (Player p : a.players()) {
            if (D.mobs(a) >= perLevelCap * a.level || a.rnd.nextInt(100) >= chance) continue;
            Location l = Util.ring(p.getLocation(), 16, 28, a.rnd);
            Block b = Util.top(a.world, l.getBlockX(), l.getBlockZ());
            if (b == null || b.isLiquid()) continue;
            EntityType[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.HUSK, EntityType.STRAY};
            Entity e = a.world.spawnEntity(b.getLocation().add(0.5, 1, 0.5), types[a.rnd.nextInt(types.length)]);
            Util.tag(e, a);
            if (e instanceof org.bukkit.entity.Mob mob) mob.setTarget(p);
        }
    }

    // ------------------------------------------------------------------ Eclipse
    private static Disaster eclipse() {
        return new Disaster("eclipse", "Eclipse total", "Cielo", Material.BLACK_CONCRETE, 6, "§8",
                "El sol se apaga: noche en pleno día y los monstruos no arden con la luz.") {
            @Override
            public List<String> conflicts() {
                return List.of("luna_sangre", "ola_calor");
            }

            @Override
            public void start(Active a) {
                D.night(a);
            }

            @Override
            public void tick(Active a) {
                D.keepNight(a);
                horde(a, 4, 35);
                for (Player p : a.players()) if (a.beat % 50 == 0) p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 0.7f);
            }

            @Override
            public void stop(Active a) {
                D.endNight(a);
            }
        };
    }

    // ------------------------------------------------------------------ Luna de sangre
    private static Disaster lunaSangre() {
        return new Disaster("luna_sangre", "Luna de sangre", "Cielo", Material.REDSTONE_BLOCK, 8, "§4",
                "Noche roja: los monstruos son más fuertes y rápidos, pero sueltan doble experiencia y botín.") {
            @Override
            public List<String> conflicts() {
                return List.of("eclipse", "ola_calor");
            }

            @Override
            public void start(Active a) {
                D.night(a);
            }

            @Override
            public void tick(Active a) {
                D.keepNight(a);
                horde(a, 5, 30);
                for (Player p : a.players()) {
                    a.world.spawnParticle(Particle.DUST, p.getLocation().add(0, 2, 0), 12, 6, 3, 6, new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.6f));
                    if (a.beat % 60 == 0) {
                        p.playSound(p.getLocation(), Sound.ENTITY_WOLF_HOWL, 1f, 0.6f);
                        Util.actionbar(p, "§4🌑 Luna de sangre: §7los monstruos son más fuertes... y más generosos");
                    }
                }
            }

            @Override
            public void stop(Active a) {
                D.endNight(a);
            }
        };
    }

    // ------------------------------------------------------------------ Gravedad lunar
    private static Disaster gravedad() {
        return new Disaster("gravedad", "Gravedad lunar", "Cielo", Material.FEATHER, 4, "§5",
                "La gravedad se debilita: saltos enormes y caídas suaves. Divertido y peligroso.") {
            @Override
            public int every() {
                return 20;
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.world.getPlayers()) {
                    D.eff(p, PotionEffectType.JUMP_BOOST, 70, 1 + a.level * 2);
                    D.eff(p, PotionEffectType.SLOW_FALLING, 70, 0);
                    a.world.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 6, 1.5, 1, 1.5, 0.01);
                }
            }
        };
    }

    // ------------------------------------------------------------------ Tormenta magnética
    private static Disaster magnetica() {
        return new Disaster("tormenta_magnetica", "Tormenta magnética", "Cielo", Material.LODESTONE, 5, "§e",
                "El campo magnético se vuelve loco: nada de élitros ni perlas, y el metal te electriza.") {
            @Override
            public String warning() {
                return "Una tormenta magnética inutilizará élitros y teletransportes. Quitaos las armaduras metálicas.";
            }

            @Override
            public int every() {
                return 5;
            }

            private int metal(Player p) {
                int n = 0;
                for (ItemStack it : p.getInventory().getArmorContents()) {
                    if (it == null) continue;
                    String t = it.getType().name();
                    if (t.startsWith("IRON_") || t.startsWith("CHAINMAIL_") || t.startsWith("GOLDEN_") || t.startsWith("DIAMOND_") || t.startsWith("NETHERITE_")) n++;
                }
                return n;
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.world.getPlayers()) {
                    a.world.spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation().add(0, 1, 0), 6, 0.6, 0.8, 0.6, 0.2);
                    if (!Util.valid(p)) continue;
                    if (p.isGliding()) {
                        p.setGliding(false);
                        Util.actionbar(p, "§e⚡ El campo magnético apaga tus élitros");
                    }
                    int m = metal(p);
                    if (m > 0 && a.beat % 25 == 0 && a.rnd.nextInt(100) < 20 * a.level) {
                        p.damage(1.0 * m);
                        p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2f);
                        Util.actionbar(p, "§e⚡ ¡El metal de tu armadura te electriza!");
                    }
                }
                if (a.beat % 50 == 0) for (Player p : a.world.getPlayers()) D.lightning(a.world, Util.ring(p.getLocation(), 15, 40, a.rnd), false);
            }
        };
    }

    // ------------------------------------------------------------------ Fantasmas
    private static Disaster fantasmas() {
        return new Disaster("fantasmas", "Horda de fantasmas", "Cielo", Material.PHANTOM_MEMBRANE, 6, "§9",
                "Fantasmas gigantes surcan el cielo y se lanzan en picado, de día y de noche.") {
            @Override
            public String warning() {
                return "Una bandada de fantasmas se acerca. Un techo sobre la cabeza es vuestro mejor amigo.";
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.players()) {
                    if (D.mobs(a) >= 3 * a.level || a.rnd.nextInt(100) >= 40) continue;
                    Location l = p.getLocation().add(a.rnd.nextInt(21) - 10, 12 + a.rnd.nextInt(6), a.rnd.nextInt(21) - 10);
                    if (!l.getBlock().getType().isAir()) continue;
                    a.world.spawn(l, org.bukkit.entity.Phantom.class, ph -> {
                        Util.tag(ph, a);
                        ph.setSize(Math.min(6, 1 + a.level * 2));
                        ph.setTarget(p);
                    });
                    a.world.spawnParticle(Particle.SOUL, l, 12, 0.5, 0.5, 0.5, 0.05);
                    p.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1f, 0.6f);
                }
            }
        };
    }
}
