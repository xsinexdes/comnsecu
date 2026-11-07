package com.etheria.consecuencias.disasters;

import com.etheria.consecuencias.Active;
import com.etheria.consecuencias.Disaster;
import com.etheria.consecuencias.Gear;
import com.etheria.consecuencias.DisasterManager;
import com.etheria.consecuencias.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Tag;

import java.util.ArrayList;
import java.util.List;

/** Clima: sequía, inundación, diluvio, lluvia ácida, tormenta eléctrica, huracán, tornado, granizo, calor, ventisca, niebla, arena. */
public final class WeatherDisasters {
    private WeatherDisasters() {
    }

    public static void register(DisasterManager m) {
        m.register(sequia());
        m.register(inundacion());
        m.register(diluvio());
        m.register(lluviaAcida());
        m.register(tormentaElectrica());
        m.register(huracan());
        m.register(tornado());
        m.register(granizo());
        m.register(olaCalor());
        m.register(ventisca());
        m.register(niebla());
        m.register(tormentaArena());
    }

    // ------------------------------------------------------------------ Sequía
    private static Disaster sequia() {
        return new Disaster("sequia", "Sequía", "Clima", Material.DEAD_BUSH, 10, "§e",
                "El agua se evapora, los cultivos dejan de crecer y los jugadores pasan sed.") {
            @Override
            public String warning() {
                return "Los ríos y lagos se secarán. Guardad agua y comida.";
            }

            @Override
            public List<String> conflicts() {
                return List.of("diluvio", "inundacion", "lluvia_acida", "ventisca");
            }

            @Override
            public int every() {
                return 3;
            }

            @Override
            public void start(Active a) {
                a.world.setStorm(false);
                a.world.setThundering(false);
                a.world.setClearWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 30 == 0) {
                    a.world.setStorm(false);
                    a.world.setClearWeatherDuration(a.minutes * 1200);
                }
                int radius = D.pl().getConfig().getInt("desastres.sequia.radio", 40);
                for (Player p : a.world.getPlayers()) {
                    D.columns(a, p, 24 + 12 * a.level, radius, top -> evaporate(a, top));
                    if (Util.valid(p) && a.beat % 60 == 0) {
                        if (Util.exposed(p) && a.rnd.nextInt(100) < 25 * a.level) p.setFoodLevel(Math.max(0, p.getFoodLevel() - 1));
                        Util.actionbar(p, "§e☀ Tu garganta está seca... §7busca agua");
                    }
                    if (a.beat % 12 == 0) {
                        D.columns(a, p, 3, 16, b -> {
                            Block f = b.getType() == Material.FARMLAND ? b : b.getRelative(0, -1, 0);
                            if (f.getBlockData() instanceof Farmland fl) {
                                fl.setMoisture(0);
                                f.setBlockData(fl, false);
                            }
                        });
                        Location l = p.getLocation().add(a.rnd.nextInt(9) - 4, 0.5, a.rnd.nextInt(9) - 4);
                        a.world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, l, 0, 0, 0.05, 0, 1);
                    }
                }
            }

            private void evaporate(Active a, Block top) {
                Block b = top;
                int guard = 0;
                // la columna de agua: de arriba a abajo
                while (guard++ < 24 && b.getType() == Material.WATER) {
                    D.pl().ledger.set(b, Material.AIR, a);
                    b = b.getRelative(0, -1, 0);
                }
                if (guard > 1) {
                    a.world.spawnParticle(Particle.CLOUD, top.getLocation().add(0.5, 1, 0.5), 3, 0.3, 0.2, 0.3, 0.02);
                    Block under = b;
                    Material um = under.getType();
                    if (um == Material.SEAGRASS || um == Material.TALL_SEAGRASS || um == Material.KELP || um == Material.KELP_PLANT) D.pl().ledger.set(under, Material.AIR, a);
                }
            }
        };
    }

    // ------------------------------------------------------------------ Inundación
    private static Disaster inundacion() {
        return new Disaster("inundacion", "Inundación", "Clima", Material.WATER_BUCKET, 8, "§9",
                "Lluvia torrencial: el agua sube y cubre las zonas bajas.") {
            @Override
            public String warning() {
                return "El nivel del agua subirá. ¡Subid a zonas altas!";
            }

            @Override
            public List<String> conflicts() {
                return List.of("sequia", "ola_calor");
            }

            @Override
            public int every() {
                return 4;
            }

            @Override
            public void start(Active a) {
                a.world.setStorm(true);
                a.world.setThundering(a.level >= 2);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 40 == 0) a.world.setWeatherDuration(a.minutes * 1200);
                int rise = 3 + a.level * 2;
                double f = Math.min(1.0, a.progress() / 0.7);
                for (Player p : a.world.getPlayers()) {
                    int floodY = D.baseY(a, p) + (int) Math.round(f * rise);
                    D.flood(a, p, floodY, 40 + 20 * a.level, 40);
                    if (a.beat % 12 == 0) {
                        a.world.spawnParticle(Particle.FALLING_WATER, p.getLocation().add(0, 3, 0), 30, 5, 1, 5, 0);
                        p.playSound(p.getLocation(), Sound.WEATHER_RAIN, 0.6f, 0.8f);
                    }
                    if (Util.valid(p) && a.beat % 40 == 0) Util.actionbar(p, "§9🌊 El agua sube... §7nivel Y=" + floodY + " §8(tú: " + p.getLocation().getBlockY() + ")");
                }
            }
        };
    }

    // ------------------------------------------------------------------ Diluvio
    private static Disaster diluvio() {
        return new Disaster("diluvio", "Diluvio", "Clima", Material.PRISMARINE_SHARD, 8, "§3",
                "Lluvia brutal con rayos y ahogados que salen a cazar.") {
            @Override
            public List<String> conflicts() {
                return List.of("sequia", "ola_calor");
            }

            @Override
            public void start(Active a) {
                a.world.setStorm(true);
                a.world.setThundering(true);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 60 == 0) a.world.setWeatherDuration(a.minutes * 1200);
                for (Player p : a.players()) {
                    if (a.beat % 20 == 0) D.flood(a, p, D.baseY(a, p) + a.level, 14 + 6 * a.level, 30); // charcos y zonas bajas anegadas
                    if (Util.exposed(p)) {
                        D.eff(p, PotionEffectType.SLOWNESS, 60, 0);
                        a.world.spawnParticle(Particle.FALLING_WATER, p.getLocation().add(0, 2, 0), 20, 3, 1, 3, 0);
                        if (a.rnd.nextInt(100) < 6 * a.level) D.lightning(a.world, Util.ring(p.getLocation(), 8, 30, a.rnd), true);
                    }
                    // ahogados
                    if (D.mobs(a) < 6 * a.level && a.rnd.nextInt(100) < 25) {
                        Location l = Util.ring(p.getLocation(), 14, 26, a.rnd);
                        Block b = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                        if (b != null) {
                            Location sp = b.getLocation().add(0.5, 1, 0.5);
                            a.world.spawn(sp, Drowned.class, d -> {
                                Util.tag(d, a);
                                d.setTarget(p);
                            });
                        }
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Lluvia ácida
    private static Disaster lluviaAcida() {
        return new Disaster("lluvia_acida", "Lluvia ácida", "Clima", Material.SLIME_BALL, 8, "§a",
                "La lluvia quema: daña a quien no se cubra, corroe los cascos y marchita las plantas.") {
            @Override
            public String warning() {
                return "Buscad refugio bajo techo. La lluvia corroe la piel y las plantas.";
            }

            @Override
            public List<String> conflicts() {
                return List.of("sequia", "ola_calor");
            }

            @Override
            public void start(Active a) {
                a.world.setStorm(true);
                a.world.setThundering(false);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 60 == 0) a.world.setWeatherDuration(a.minutes * 1200);
                for (Player p : a.world.getPlayers()) {
                    a.world.spawnParticle(Particle.DUST, p.getLocation().add(0, 3, 0), 15, 4, 1, 4, new Particle.DustOptions(Color.fromRGB(110, 255, 60), 1.2f));
                    if (Util.valid(p) && Util.exposed(p) && a.beat % 20 == 0) {
                        ItemStack h = p.getInventory().getHelmet();
                        if (Gear.hasMask(p)) {
                            Gear.wearMask(p, 2 * a.level);
                            Util.actionbar(p, "§a🛡 La máscara te protege de la lluvia ácida");
                        } else if (h != null && h.getItemMeta() instanceof Damageable dm && h.getType().getMaxDurability() > 0) {
                            dm.setDamage(Math.min(h.getType().getMaxDurability() - 1, dm.getDamage() + 3 * a.level));
                            h.setItemMeta(dm);
                            Util.actionbar(p, "§a☣ ¡La lluvia corroe tu casco!");
                        } else {
                            p.damage(a.level);
                            Util.actionbar(p, "§a☣ ¡La lluvia ácida te quema! §7Busca un techo");
                        }
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.4f, 1.4f);
                    }
                    D.columns(a, p, 4 * a.level, 30, top -> {
                        Material t = top.getType();
                        if (Tag.LEAVES.isTagged(t) || Tag.FLOWERS.isTagged(t) || Tag.CROPS.isTagged(t) || t == Material.SHORT_GRASS || t == Material.TALL_GRASS) {
                            if (a.rnd.nextInt(100) < 40) D.pl().ledger.set(top, Material.AIR, a);
                        }
                    });
                }
            }
        };
    }

    // ------------------------------------------------------------------ Tormenta eléctrica
    private static Disaster tormentaElectrica() {
        return new Disaster("tormenta_electrica", "Tormenta eléctrica", "Clima", Material.LIGHTNING_ROD, 6, "§e",
                "Rayos por todas partes. Cuidado con los campos abiertos.") {
            @Override
            public List<String> conflicts() {
                return List.of("sequia");
            }

            @Override
            public void start(Active a) {
                a.world.setStorm(true);
                a.world.setThundering(true);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
                a.world.setThunderDuration(a.minutes * 1200 + 600);
            }

            @Override
            public int every() {
                return 5;
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 60 == 0) {
                    a.world.setWeatherDuration(a.minutes * 1200);
                    a.world.setThunderDuration(a.minutes * 1200);
                }
                for (Player p : a.world.getPlayers()) {
                    if (a.rnd.nextInt(100) < 14 * a.level) D.lightning(a.world, Util.ring(p.getLocation(), 6, 32, a.rnd), true);
                    if (Util.valid(p) && Util.exposed(p) && a.rnd.nextInt(1000) < 6 * a.level) {
                        Util.actionbar(p, "§e⚡ ¡El aire se carga! §7Agáchate o busca refugio");
                        D.lightning(a.world, Util.ring(p.getLocation(), 1, 3, a.rnd), true);
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Huracán
    private static Disaster huracan() {
        return new Disaster("huracan", "Huracán", "Clima", Material.FEATHER, 6, "§b",
                "Vientos huracanados que arrastran a quien esté al descubierto.") {
            @Override
            public void start(Active a) {
                a.world.setStorm(true);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
                a.data.put("ang", a.rnd.nextDouble() * Math.PI * 2);
            }

            @Override
            public int every() {
                return 2;
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 60 == 0) a.world.setWeatherDuration(a.minutes * 1200);
                double ang = a.get("ang", 0.0) + (a.rnd.nextDouble() - 0.5) * 0.04;
                a.data.put("ang", ang);
                Vector wind = new Vector(Math.cos(ang), 0, Math.sin(ang));
                for (Player p : a.world.getPlayers()) {
                    a.world.spawnParticle(Particle.CLOUD, p.getLocation().add(wind.clone().multiply(-6)).add(0, 1.5, 0), 8, 1, 1.5, 1, 0.4, null);
                    if (!Util.valid(p) || !Util.exposed(p) || p.isSneaking() || p.isGliding()) continue;
                    double f = 0.05 + 0.04 * a.level;
                    Vector v = p.getVelocity().add(wind.clone().multiply(f));
                    if (v.getY() > 0.5) v.setY(0.5);
                    p.setVelocity(v);
                    if (a.beat % 30 == 0) {
                        p.playSound(p.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.5f, 0.5f);
                        Util.actionbar(p, "§b💨 ¡El viento te arrastra! §7Agáchate (shift) para resistir");
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Tornado
    private static Disaster tornado() {
        return new Disaster("tornado", "Tornado", "Clima", Material.COBWEB, 5, "§7",
                "Embudos que recorren el mundo lanzando por los aires todo lo que encuentran.") {
            @Override
            public String warning() {
                return "Uno o varios tornados avanzan sobre la zona. Corred perpendicularmente a su camino.";
            }

            @Override
            public int every() {
                return 1;
            }

            @Override
            public void start(Active a) {
                List<double[]> t = new ArrayList<>();
                List<Player> ps = a.world.getPlayers();
                for (int i = 0; i < a.level; i++) {
                    Location base = ps.isEmpty() ? a.world.getSpawnLocation() : ps.get(a.rnd.nextInt(ps.size())).getLocation();
                    Location l = Util.ring(base, 45, 75, a.rnd);
                    double ang = a.rnd.nextDouble() * Math.PI * 2;
                    t.add(new double[]{l.getX(), l.getZ(), ang});
                }
                a.data.put("t", t);
                a.world.setStorm(true);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public void stop(Active a) {
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                List<double[]> ts = (List<double[]>) a.data.get("t");
                if (ts == null) return;
                boolean grief = D.pl().getConfig().getBoolean("destruccion", true);
                for (double[] t : ts) {
                    t[2] += (a.rnd.nextDouble() - 0.5) * 0.12;
                    t[0] += Math.cos(t[2]) * 0.28;
                    t[1] += Math.sin(t[2]) * 0.28;
                    int cx = (int) Math.floor(t[0]), cz = (int) Math.floor(t[1]);
                    Block top = Util.top(a.world, cx, cz);
                    if (top == null) {
                        t[2] += Math.PI; // vuelve hacia atrás si sale del área cargada
                        continue;
                    }
                    double gy = top.getY() + 1;
                    // embudo: dos brazos en espiral que se ensanchan, escombros, disco de nube arriba y polvo en la base
                    for (int h = 0; h < 26; h++) {
                        double r = 0.8 + h * 0.12 + h * h * 0.014, ang = a.beat * 0.55 + h * 0.6;
                        for (int arm = 0; arm < (h % 2 == 0 ? 2 : 1); arm++) {
                            double an = ang + arm * Math.PI;
                            Location pl = new Location(a.world, t[0] + Math.cos(an) * r, gy + h, t[1] + Math.sin(an) * r);
                            a.world.spawnParticle(Particle.CLOUD, pl, 1, 0.1, 0.1, 0.1, 0);
                            if (h % 3 == 0) a.world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pl, 0, 0, 0.1, 0, 1);
                            if (h % 5 == 0) a.world.spawnParticle(Particle.FALLING_DUST, pl, 1, 0.2, 0.2, 0.2, 0, top.getBlockData());
                        }
                    }
                    if (a.beat % 3 == 0) {
                        for (int k = 0; k < 8; k++) {
                            double an = Math.PI * 2 * k / 8 + a.beat * 0.2;
                            a.world.spawnParticle(Particle.LARGE_SMOKE, t[0] + Math.cos(an) * 8, gy + 26, t[1] + Math.sin(an) * 8, 2, 0.8, 0.3, 0.8, 0.01);
                        }
                        D.ring(a.world, new Location(a.world, t[0], gy - 0.2, t[1]), 2.5 + a.rnd.nextDouble() * 2, Particle.CLOUD, 10);
                        a.world.spawnParticle(Particle.BLOCK, t[0], gy + 0.3, t[1], 14, 2.5, 0.2, 2.5, 0.2, top.getBlockData());
                    }
                    if (a.rnd.nextInt(100) < 2) a.world.strikeLightningEffect(new Location(a.world, t[0] + a.rnd.nextInt(15) - 7, gy, t[1] + a.rnd.nextInt(15) - 7));
                    if (a.beat % 12 == 0) {
                        a.world.playSound(new Location(a.world, t[0], gy, t[1]), Sound.ENTITY_PHANTOM_FLAP, 3f, 0.5f);
                        a.world.playSound(new Location(a.world, t[0], gy, t[1]), Sound.ITEM_ELYTRA_FLYING, 4f, 0.5f);
                    }
                    if (a.beat % 20 == 0) {
                        for (Player pp : a.players()) {
                            double dx = t[0] - pp.getLocation().getX(), dz = t[1] - pp.getLocation().getZ();
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            if (dist < 90) Util.actionbar(pp, "§7🌪 Tornado a §f" + (int) dist + " §7bloques al §f" + D.compass(dx, dz) + (dist < 25 ? " §c¡PELIGRO!" : ""));
                        }
                    }
                    Location center = new Location(a.world, t[0], gy + 6, t[1]);
                    // succión
                    for (Entity e : a.world.getNearbyEntities(center, 11, 14, 11)) {
                        if (e instanceof Player pp && !Util.valid(pp)) continue;
                        if (!(e instanceof LivingEntity) && !(e instanceof org.bukkit.entity.Item)) continue;
                        Vector to = center.toVector().subtract(e.getLocation().toVector());
                        double dist = Math.max(0.5, to.length());
                        Vector tang = new Vector(-to.getZ(), 0, to.getX()).normalize().multiply(0.42);
                        Vector v = to.normalize().multiply(0.22).add(tang).setY(0.25 + 0.08 * a.level);
                        e.setVelocity(e.getVelocity().multiply(0.4).add(v));
                        if (dist < 4 && e instanceof LivingEntity le && a.beat % 10 == 0) le.damage(1 + a.level);
                    }
                    // arranca bloques blandos
                    if (grief && a.beat % 2 == 0) {
                        for (int i = 0; i < 2 + a.level; i++) {
                            int bx = cx + a.rnd.nextInt(11) - 5, bz = cz + a.rnd.nextInt(11) - 5;
                            Block tb = Util.top(a.world, bx, bz);
                            if (tb == null || !D.soft(tb.getType()) || tb.getType() == Material.SNOW) continue;
                            org.bukkit.block.data.BlockData bd = tb.getBlockData();
                            if (D.pl().ledger.set(tb, Material.AIR, a)) {
                                FallingBlock fb = a.world.spawnFallingBlock(tb.getLocation().add(0.5, 0.2, 0.5), bd);
                                fb.setDropItem(false);
                                fb.setHurtEntities(true);
                                Util.tag(fb, a);
                                fb.setVelocity(new Vector((a.rnd.nextDouble() - 0.5) * 0.8, 0.9 + a.rnd.nextDouble() * 0.6, (a.rnd.nextDouble() - 0.5) * 0.8));
                            }
                        }
                    }
                    // aviso de cercanía
                    if (a.beat % 20 == 0) {
                        for (Player p : a.players()) {
                            double d = Math.hypot(p.getLocation().getX() - t[0], p.getLocation().getZ() - t[1]);
                            if (d < 60) Util.actionbar(p, "§7🌪 Tornado a §f" + (int) d + " §7bloques" + (d < 25 ? " §c¡HUYE!" : ""));
                        }
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Granizo
    private static Disaster granizo() {
        return new Disaster("granizo", "Granizo", "Clima", Material.SNOWBALL, 5, "§f",
                "Piedras de hielo caen del cielo: daña a quien esté al aire libre y rompe cristales.") {
            @Override
            public void start(Active a) {
                a.world.setStorm(true);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public int every() {
                return 4;
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 60 == 0) a.world.setWeatherDuration(a.minutes * 1200);
                for (Player p : a.world.getPlayers()) {
                    for (int i = 0; i < 2 * a.level; i++) {
                        Location l = p.getLocation().add(a.rnd.nextInt(11) - 5, 8 + a.rnd.nextInt(5), a.rnd.nextInt(11) - 5);
                        a.world.spawnParticle(Particle.ITEM_SNOWBALL, l, 6, 0.1, 0.6, 0.1, 0.0);
                    }
                    if (Util.valid(p) && Util.exposed(p) && a.rnd.nextInt(100) < 35) {
                        double dmg = p.getInventory().getHelmet() != null ? 1 : 2 + (a.level > 2 ? 1 : 0);
                        p.damage(dmg);
                        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_HIT, 0.8f, 1.6f);
                        Util.actionbar(p, "§f❄ ¡Granizo! §7Un casco ayuda");
                    }
                }
                if (a.beat % 12 == 0) {
                    for (Player p : a.world.getPlayers()) {
                        D.columns(a, p, 3 * a.level, 26, top -> {
                            Material t = top.getType();
                            if ((t == Material.GLASS || t == Material.GLASS_PANE || Tag.IMPERMEABLE.isTagged(t)) && a.rnd.nextInt(100) < 45) {
                                if (D.pl().ledger.set(top, Material.AIR, a)) a.world.playSound(top.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                            }
                        });
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Ola de calor
    private static Disaster olaCalor() {
        return new Disaster("ola_calor", "Ola de calor", "Clima", Material.BLAZE_POWDER, 8, "§6",
                "El sol abrasa: quien esté al descubierto sin agua o sombra se deshidrata y se quema.") {
            @Override
            public List<String> conflicts() {
                return List.of("diluvio", "inundacion", "ventisca", "lluvia_acida");
            }

            @Override
            public void start(Active a) {
                a.world.setStorm(false);
                a.world.setThundering(false);
                a.world.setClearWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 60 == 0) a.world.setClearWeatherDuration(a.minutes * 1200);
                @SuppressWarnings("unchecked")
                java.util.Map<java.util.UUID, Integer> heat = (java.util.Map<java.util.UUID, Integer>) a.data.computeIfAbsent("calor", k -> new java.util.HashMap<java.util.UUID, Integer>());
                long time = a.world.getTime();
                boolean day = time < 12500;
                for (Player p : a.players()) {
                    int h = heat.getOrDefault(p.getUniqueId(), 0);
                    boolean cool = Util.inWater(p) || !Util.exposed(p) || p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) || !day;
                    if (cool) h = Math.max(0, h - 3);
                    else h += a.level;
                    heat.put(p.getUniqueId(), h);
                    if (h > 8) {
                        a.world.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1.2, 0), 2, 0.3, 0.4, 0.3, 0.01);
                        if (h % 3 == 0) Util.actionbar(p, "§6☀ Calor extremo: §7busca sombra o agua §8(" + Math.min(100, h * 100 / 30) + "%)");
                        D.eff(p, PotionEffectType.SLOWNESS, 40, 0);
                    }
                    if (h > 18 && h % 2 == 0) D.eff(p, PotionEffectType.NAUSEA, 80, 0);
                    if (h > 30) {
                        p.damage(1.0 * a.level);
                        p.setFireTicks(Math.max(p.getFireTicks(), 20));
                        heat.put(p.getUniqueId(), 22);
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Ventisca
    private static Disaster ventisca() {
        return new Disaster("ventisca", "Ventisca", "Clima", Material.POWDER_SNOW_BUCKET, 8, "§b",
                "Frío mortal: el hielo cubre el agua y la nieve el suelo. Hay que buscar fuego.") {
            @Override
            public List<String> conflicts() {
                return List.of("sequia", "ola_calor");
            }

            @Override
            public void start(Active a) {
                a.world.setStorm(true);
                a.world.setWeatherDuration(a.minutes * 1200 + 600);
            }

            @Override
            public int every() {
                return 5;
            }

            private boolean nearHeat(Player p) {
                Block c = p.getLocation().getBlock();
                for (int x = -3; x <= 3; x++) for (int y = -1; y <= 2; y++) for (int z = -3; z <= 3; z++) {
                    Material t = c.getRelative(x, y, z).getType();
                    if (t == Material.CAMPFIRE || t == Material.SOUL_CAMPFIRE || t == Material.FIRE || t == Material.LAVA || t == Material.MAGMA_BLOCK || t == Material.FURNACE || t == Material.BLAST_FURNACE || t == Material.SMOKER) return true;
                }
                return false;
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 60 == 0) a.world.setWeatherDuration(a.minutes * 1200);
                for (Player p : a.world.getPlayers()) {
                    a.world.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 2, 0), 30, 5, 2, 5, 0.02);
                    if (Util.valid(p)) {
                        boolean warm = nearHeat(p);
                        if (Gear.hasCoat(p)) {
                            p.setFreezeTicks(Math.max(0, p.getFreezeTicks() - 20));
                        } else if (Util.exposed(p) && !warm) {
                            p.setFreezeTicks(Math.min(p.getMaxFreezeTicks() + 40, p.getFreezeTicks() + 14 * a.level));
                            if (a.level >= 2) D.eff(p, PotionEffectType.SLOWNESS, 40, 0);
                            if (p.getFreezeTicks() > 100 && a.beat % 20 == 0) Util.actionbar(p, "§b❄ Te estás congelando: §7¡busca una hoguera!");
                        } else if (warm) {
                            p.setFreezeTicks(Math.max(0, p.getFreezeTicks() - 40));
                        }
                    }
                    D.columns(a, p, 4 * a.level, 32, top -> {
                        if (top.getType() == Material.WATER && a.rnd.nextInt(100) < 70) D.pl().ledger.set(top, Material.ICE, a);
                        else if (top.getType().isSolid() && !top.isLiquid()) {
                            Block up = top.getRelative(0, 1, 0);
                            if (up.getType().isAir() && top.getType() != Material.SNOW && top.getType().isOccluding()) D.pl().ledger.set(up, Material.SNOW, a);
                        }
                    });
                }
            }
        };
    }

    // ------------------------------------------------------------------ Niebla
    private static Disaster niebla() {
        return new Disaster("niebla", "Niebla espesa", "Clima", Material.WHITE_STAINED_GLASS, 8, "§7",
                "No se ve nada y hay cosas moviéndose en la niebla.") {
            @Override
            public int every() {
                return 10;
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.players()) {
                    D.eff(p, PotionEffectType.DARKNESS, 240, 0);
                    if (a.level >= 3) D.eff(p, PotionEffectType.BLINDNESS, 30, 0);
                    a.world.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 40, 9, 2, 9, 0.003);
                    if (D.mobs(a) < 4 * a.level && a.rnd.nextInt(100) < 30 && Util.exposed(p)) {
                        Location l = Util.ring(p.getLocation(), 14, 24, a.rnd);
                        Block b = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                        if (b != null && !b.isLiquid()) {
                            EntityType[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.STRAY};
                            Entity e = a.world.spawnEntity(b.getLocation().add(0.5, 1, 0.5), types[a.rnd.nextInt(types.length)]);
                            Util.tag(e, a);
                            if (e instanceof org.bukkit.entity.Mob mob) mob.setTarget(p);
                        }
                    }
                    if (a.beat % 60 == 0) p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 0.6f, 0.7f);
                }
            }
        };
    }

    // ------------------------------------------------------------------ Tormenta de arena
    private static Disaster tormentaArena() {
        return new Disaster("tormenta_arena", "Tormenta de arena", "Clima", Material.SAND, 5, "§6",
                "Arena y polvo cegadores: ralentizan y ciegan a quien no lleve casco.") {
            @Override
            public int every() {
                return 5;
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.world.getPlayers()) {
                    a.world.spawnParticle(Particle.DUST, p.getLocation().add(0, 1.5, 0), 40, 7, 2, 7, new Particle.DustOptions(Color.fromRGB(205, 175, 110), 1.8f));
                    if (!Util.valid(p) || !Util.exposed(p)) continue;
                    if (Gear.hasMask(p)) {
                        if (a.beat % 40 == 0) Gear.wearMask(p, 1);
                        continue;
                    }
                    D.eff(p, PotionEffectType.SLOWNESS, 40, a.level > 1 ? 1 : 0);
                    if (p.getInventory().getHelmet() == null && a.beat % 20 == 0) {
                        D.eff(p, PotionEffectType.BLINDNESS, 50, 0);
                        Util.actionbar(p, "§6🏜 La arena te ciega: §7ponte un casco");
                    }
                    if (a.beat % 40 == 0) p.playSound(p.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.4f, 0.4f);
                }
            }
        };
    }
}
