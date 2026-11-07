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
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Tierra: terremoto, sumidero, derrumbe, erupción, incendio forestal. */
public final class EarthDisasters {
    private EarthDisasters() {
    }

    public static void register(DisasterManager m) {
        m.register(terremoto());
        m.register(sumidero());
        m.register(derrumbe());
        m.register(erupcion());
        m.register(incendio());
    }

    private static boolean ground(Material m) {
        return m == Material.STONE || m == Material.DIRT || m == Material.GRASS_BLOCK || m == Material.COARSE_DIRT || m == Material.SAND
                || m == Material.GRAVEL || m == Material.ANDESITE || m == Material.DIORITE || m == Material.GRANITE || m == Material.DEEPSLATE
                || m == Material.PODZOL || m == Material.RED_SAND || m == Material.SANDSTONE || m == Material.SNOW_BLOCK || m == Material.TUFF
                || m == Material.TERRACOTTA || m == Material.MYCELIUM || m == Material.COBBLESTONE || m == Material.NETHERRACK;
    }

    // ------------------------------------------------------------------ Terremoto
    private static Disaster terremoto() {
        return new Disaster("terremoto", "Terremoto", "Tierra", Material.COBBLESTONE_STAIRS, 3, "§6",
                "El suelo tiembla, se abren grietas y se desploma lo que no está bien cimentado.") {
            @Override
            public String warning() {
                return "Se avecina un temblor. Alejaos de acantilados y construcciones frágiles.";
            }

            @Override
            public int every() {
                return 2;
            }

            @Override
            public void start(Active a) {
                List<double[]> f = new ArrayList<>();
                List<Player> ps = a.world.getPlayers();
                for (int i = 0; i < a.level && !ps.isEmpty(); i++) {
                    Location l = Util.ring(ps.get(a.rnd.nextInt(ps.size())).getLocation(), 8, 30, a.rnd);
                    f.add(new double[]{l.getX(), l.getZ(), a.rnd.nextDouble() * Math.PI * 2, 0});
                }
                a.data.put("fisuras", f);
                a.data.put("mag", 4.0 + a.level * 0.8);
                for (double[] x : f) {
                    Location e = new Location(a.world, x[0], a.world.getHighestBlockYAt((int) x[0], (int) x[1]) + 1, x[1]);
                    D.shockwave(a.world, e, 14 + 4 * a.level, Particle.CLOUD);
                }
                for (Player p : a.world.getPlayers()) p.sendTitle("§6§l¡TERREMOTO!", "§7Agáchate y aléjate de los edificios", 5, 50, 20);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                for (Player p : a.world.getPlayers()) {
                    if (a.beat % 8 == 0) {
                        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.35f, 0.5f);
                        p.playSound(p.getLocation(), Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
                    }
                    if (a.beat % 10 == 0 && Util.valid(p)) {
                        double mag = a.get("mag", 5.0) + Math.sin(a.beat / 7.0) * 0.6 + a.rnd.nextDouble() * 0.3;
                        Util.actionbar(p, "§6〰 Sismo §7magnitud §e" + String.format(java.util.Locale.ROOT, "%.1f", mag) + " §8| §7¡mantente lejos de acantilados!");
                        Location pl = p.getLocation();
                        for (int i = 0; i < 4; i++) {
                            Location up = pl.clone().add(a.rnd.nextInt(13) - 6, 5 + a.rnd.nextInt(5), a.rnd.nextInt(13) - 6);
                            a.world.spawnParticle(Particle.FALLING_DUST, up, 6, 0.6, 0.3, 0.6, 0, pl.clone().subtract(0, 1, 0).getBlock().getBlockData());
                        }
                        if (Util.underground(p)) {
                            a.world.spawnParticle(Particle.FALLING_DUST, pl.clone().add(0, 2.5, 0), 30, 2, 0.3, 2, 0, Material.STONE.createBlockData());
                            a.world.playSound(pl, Sound.BLOCK_STONE_BREAK, 1f, 0.3f);
                            if (a.rnd.nextInt(100) < 5 * a.level) p.damage(2.0);
                        }
                    }
                    if (!Util.valid(p)) continue;
                    if (p.isOnGround() && a.beat % 4 == 0) {
                        double k = 0.1 + 0.07 * a.level;
                        p.setVelocity(p.getVelocity().add(new Vector((a.rnd.nextDouble() - 0.5) * k, 0.08, (a.rnd.nextDouble() - 0.5) * k)));
                    }
                    if (a.beat % 20 == 0) {
                        D.eff(p, PotionEffectType.NAUSEA, 120, 0);
                        a.world.spawnParticle(Particle.BLOCK, p.getLocation().add(0, 0.2, 0), 25, 2.5, 0.1, 2.5, 0.05, p.getLocation().subtract(0, 1, 0).getBlock().getBlockData());
                    }
                }
                List<double[]> fs = (List<double[]>) a.data.get("fisuras");
                if (fs == null || a.beat % 4 != 0) return;
                for (double[] f : fs) {
                    if (f[3] > 14 + 6 * a.level) continue;
                    f[0] += Math.cos(f[2]);
                    f[1] += Math.sin(f[2]);
                    f[2] += (a.rnd.nextDouble() - 0.5) * 0.5;
                    f[3]++;
                    Block top = Util.top(a.world, (int) f[0], (int) f[1]);
                    if (top == null) continue;
                    for (int d = 0; d < 3 + a.level; d++) {
                        Block b = top.getRelative(0, -d, 0);
                        if (ground(b.getType()) || D.soft(b.getType()) || Tag.LOGS.isTagged(b.getType())) D.pl().ledger.set(b, Material.AIR, a);
                    }
                    a.world.spawnParticle(Particle.SMOKE, top.getLocation().add(0.5, 1, 0.5), 8, 0.4, 0.3, 0.4, 0.02);
                    a.world.spawnParticle(Particle.LARGE_SMOKE, top.getLocation().add(0.5, 1.5, 0.5), 5, 0.6, 0.8, 0.6, 0.03);
                    a.world.spawnParticle(Particle.BLOCK, top.getLocation().add(0.5, 1, 0.5), 25, 1, 0.4, 1, 0.15, top.getBlockData());
                    a.world.spawnParticle(Particle.DUST, top.getLocation().add(0.5, 2, 0.5), 6, 0.8, 1.2, 0.8, 0, new Particle.DustOptions(org.bukkit.Color.fromRGB(120, 90, 60), 2f));
                    a.world.playSound(top.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
                }
            }
        };
    }

    // ------------------------------------------------------------------ Sumidero
    private static Disaster sumidero() {
        return new Disaster("sumidero", "Sumidero", "Tierra", Material.DRIPSTONE_BLOCK, 3, "§8",
                "El suelo se hunde formando un pozo enorme cerca de los jugadores.") {
            @Override
            public String warning() {
                return "El suelo cerca de vosotros va a ceder. ¡Moveos!";
            }

            @Override
            public int every() {
                return 10;
            }

            @Override
            public void start(Active a) {
                List<Player> ps = a.players();
                Location base = ps.isEmpty() ? a.world.getSpawnLocation() : ps.get(a.rnd.nextInt(ps.size())).getLocation();
                Location l = Util.ring(base, 8, 22, a.rnd);
                Block top = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                if (top == null) top = a.world.getBlockAt(l);
                a.data.put("x", top.getX());
                a.data.put("y", top.getY());
                a.data.put("z", top.getZ());
                a.data.put("capa", 0);
                org.bukkit.Bukkit.getScheduler().runTask(D.pl(), () -> Announce.say(a, "Un sumidero se abre cerca de " + a.get("x", 0) + ", " + a.get("z", 0) + "."));
            }

            @Override
            public void tick(Active a) {
                int cx = a.get("x", 0), cy = a.get("y", 64), cz = a.get("z", 0), layer = a.get("capa", 0);
                int depth = 10 + 4 * a.level, r0 = 4 + 2 * a.level;
                Location c = new Location(a.world, cx + 0.5, cy, cz + 0.5);
                a.world.spawnParticle(Particle.BLOCK, c, 40, r0 / 2.0, 0.5, r0 / 2.0, 0.1, Material.DIRT.createBlockData());
                a.world.playSound(c, Sound.BLOCK_GRAVEL_BREAK, 2f, 0.4f);
                if (layer > depth) return;
                int r = Math.max(2, (int) Math.round(r0 * (1.0 - layer / (depth * 1.6))));
                int y = cy - layer;
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) continue;
                    Block b = a.world.getBlockAt(cx + x, y, cz + z);
                    Material t = b.getType();
                    if (t.isAir() || t == Material.BEDROCK || b.isLiquid()) continue;
                    D.pl().ledger.set(b, Material.AIR, a);
                }
                // también se hunde lo que hay encima del nivel del suelo (árboles, plantas)
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) continue;
                    Block b = a.world.getBlockAt(cx + x, cy + 1 + layer, cz + z);
                    if (!b.getType().isAir() && !b.isLiquid() && b.getType() != Material.BEDROCK && layer < 6) D.pl().ledger.set(b, Material.AIR, a);
                }
                a.data.put("capa", layer + 1);
            }
        };
    }

    // ------------------------------------------------------------------ Derrumbe
    private static Disaster derrumbe() {
        return new Disaster("derrumbe", "Derrumbes", "Tierra", Material.GRAVEL, 6, "§7",
                "Las cuevas y minas se desmoronan: caen techos sobre quien está bajo tierra.") {
            @Override
            public String warning() {
                return "Los túneles crujen. ¡Salid de las minas!";
            }

            @Override
            public int every() {
                return 10;
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.players()) {
                    if (!Util.underground(p)) continue;
                    if (a.beat % 20 == 0) {
                        p.playSound(p.getLocation(), Sound.BLOCK_DEEPSLATE_BREAK, 1f, 0.5f);
                        a.world.spawnParticle(Particle.BLOCK, p.getLocation().add(0, 2, 0), 18, 1.2, 0.5, 1.2, 0.05, Material.STONE.createBlockData());
                    }
                    if (a.rnd.nextInt(100) >= 22 * a.level) continue;
                    // techo sobre el jugador o cerca
                    Location base = p.getLocation().add(a.rnd.nextInt(5) - 2, 0, a.rnd.nextInt(5) - 2);
                    Block ceil = null;
                    for (int y = 2; y <= 7; y++) {
                        Block b = a.world.getBlockAt(base.getBlockX(), base.getBlockY() + y, base.getBlockZ());
                        if (b.getType().isSolid() && b.getType() != Material.BEDROCK) {
                            ceil = b;
                            break;
                        }
                    }
                    if (ceil == null) continue;
                    Util.actionbar(p, "§7⚠ ¡El techo se desmorona!");
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                        Block b = ceil.getRelative(dx, 0, dz);
                        if (!b.getType().isSolid() || b.getType() == Material.BEDROCK || b.getType().getHardness() < 0) continue;
                        BlockData bd = b.getBlockData();
                        if (D.pl().ledger.set(b, Material.AIR, a)) {
                            FallingBlock fb = a.world.spawnFallingBlock(b.getLocation().add(0.5, 0.0, 0.5), bd);
                            fb.setDropItem(false);
                            fb.setHurtEntities(true);
                            Util.tag(fb, a);
                        }
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Erupción
    private static Disaster erupcion() {
        return new Disaster("erupcion", "Erupción volcánica", "Tierra", Material.MAGMA_BLOCK, 5, "§c",
                "Surge una fisura que escupe bombas de lava y llena el aire de ceniza.") {
            @Override
            public String warning() {
                return "Una fisura volcánica va a abrirse. La ceniza y la lava llegarán lejos.";
            }

            @Override
            public int every() {
                return 3;
            }

            @Override
            public List<String> conflicts() {
                return List.of("ventisca");
            }

            @Override
            public void start(Active a) {
                List<Player> ps = a.players();
                Location base = ps.isEmpty() ? a.world.getSpawnLocation() : ps.get(a.rnd.nextInt(ps.size())).getLocation();
                Location l = Util.ring(base, 50, 85, a.rnd);
                Block top = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                if (top == null) top = a.world.getBlockAt(l);
                a.data.put("x", top.getX());
                a.data.put("y", top.getY());
                a.data.put("z", top.getZ());
                // cráter: lava en el centro y magma alrededor
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                    Block b = Util.top(a.world, top.getX() + dx, top.getZ() + dz);
                    if (b == null) continue;
                    D.pl().ledger.set(b, Math.abs(dx) + Math.abs(dz) <= 1 ? Material.LAVA : Material.MAGMA_BLOCK, a);
                }
                org.bukkit.Bukkit.getScheduler().runTask(D.pl(), () -> Announce.say(a, "Un volcán ha surgido en " + a.get("x", 0) + ", " + a.get("z", 0) + "."));
            }

            @Override
            public void tick(Active a) {
                int cx = a.get("x", 0), cy = a.get("y", 64), cz = a.get("z", 0);
                Location v = new Location(a.world, cx + 0.5, cy + 1, cz + 0.5);
                a.world.spawnParticle(Particle.LAVA, v, 12, 1, 1, 1, 0);
                a.world.spawnParticle(Particle.LARGE_SMOKE, v.clone().add(0, 3, 0), 10, 1.2, 4, 1.2, 0.02);
                a.world.spawnParticle(Particle.ASH, v.clone().add(0, 8, 0), 60, 14, 8, 14, 0);
                if (a.beat % 12 == 0) a.world.playSound(v, Sound.ENTITY_GENERIC_EXPLODE, 4f, 0.5f);
                int bombs = 1 + a.level;
                for (int i = 0; i < bombs; i++) {
                    FallingBlock fb = a.world.spawnFallingBlock(v.clone().add(0, 1, 0), Material.MAGMA_BLOCK.createBlockData());
                    fb.setDropItem(false);
                    fb.setHurtEntities(true);
                    Util.tag(fb, a);
                    double ang = a.rnd.nextDouble() * Math.PI * 2, sp = 0.35 + a.rnd.nextDouble() * (0.35 + 0.12 * a.level);
                    fb.setVelocity(new Vector(Math.cos(ang) * sp, 1.0 + a.rnd.nextDouble() * 0.7, Math.sin(ang) * sp));
                }
                for (Player p : a.world.getPlayers()) {
                    double d = Math.hypot(p.getLocation().getX() - cx, p.getLocation().getZ() - cz);
                    a.world.spawnParticle(Particle.ASH, p.getLocation().add(0, 2, 0), 25, 5, 3, 5, 0);
                    if (Util.valid(p) && d < 140 && a.beat % 30 == 0 && !Gear.hasMask(p)) {
                        D.eff(p, PotionEffectType.DARKNESS, 100, 0);
                        Util.actionbar(p, "§c🌋 Ceniza volcánica: §7volcán a §f" + (int) d + " §7bloques");
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Incendio forestal
    private static Disaster incendio() {
        return new Disaster("incendio", "Incendio forestal", "Tierra", Material.CAMPFIRE, 6, "§6",
                "Un incendio nace en el bosque y se extiende con el viento. Se apaga solo al acabar.") {
            @Override
            public String warning() {
                return "Fuego en la espesura: el humo se verá desde lejos. Mantened agua a mano.";
            }

            @Override
            public List<String> conflicts() {
                return List.of("diluvio", "inundacion", "lluvia_acida");
            }

            @Override
            public int every() {
                return 5;
            }

            @Override
            public void start(Active a) {
                List<Player> ps = a.players();
                Location base = ps.isEmpty() ? a.world.getSpawnLocation() : ps.get(a.rnd.nextInt(ps.size())).getLocation();
                // busca un punto con hojas/árboles cerca
                Block best = null;
                for (int i = 0; i < 40 && best == null; i++) {
                    Location l = Util.ring(base, 25, 70, a.rnd);
                    Block t = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                    if (t != null && (Tag.LEAVES.isTagged(t.getType()) || Tag.LOGS.isTagged(t.getType()))) best = t;
                }
                if (best == null) {
                    Location l = Util.ring(base, 25, 60, a.rnd);
                    best = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                }
                if (best == null) best = a.world.getHighestBlockAt(base);
                a.data.put("x", best.getX());
                a.data.put("z", best.getZ());
                a.data.put("r", 5.0);
                final Block bb = best;
                org.bukkit.Bukkit.getScheduler().runTask(D.pl(), () -> Announce.say(a, "Se ha declarado un incendio en " + bb.getX() + ", " + bb.getZ() + "."));
            }

            @Override
            public void tick(Active a) {
                int ox = a.get("x", 0), oz = a.get("z", 0);
                double r = a.get("r", 5.0);
                double max = 18 + 10 * a.level;
                if (r < max) a.data.put("r", r + 0.35);
                for (int i = 0; i < 4 + 3 * a.level; i++) {
                    double ang = a.rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(a.rnd.nextDouble()) * r;
                    int x = (int) (ox + Math.cos(ang) * d), z = (int) (oz + Math.sin(ang) * d);
                    Block top = Util.top(a.world, x, z);
                    if (top == null) continue;
                    Block above = top.getRelative(0, 1, 0);
                    Material t = top.getType();
                    boolean burnable = Tag.LEAVES.isTagged(t) || Tag.LOGS.isTagged(t) || Tag.PLANKS.isTagged(t) || t == Material.GRASS_BLOCK || Tag.WOOL.isTagged(t) || D.soft(t);
                    if (burnable && above.getType().isAir()) {
                        D.pl().ledger.set(above, Material.FIRE, a);
                    } else if (Tag.LEAVES.isTagged(t) && top.getRelative(0, 0, 0).getType() != Material.FIRE) {
                        Block up = top.getRelative(0, 1, 0);
                        if (up.getType().isAir()) D.pl().ledger.set(up, Material.FIRE, a);
                    }
                    a.world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, top.getLocation().add(0.5, 2, 0.5), 0, 0, 0.08, 0, 1);
                }
                for (Player p : a.players()) {
                    double d = Math.hypot(p.getLocation().getX() - ox, p.getLocation().getZ() - oz);
                    if (d < 80 && a.beat % 40 == 0) Util.actionbar(p, "§6🔥 Incendio a §f" + (int) d + " §7bloques");
                    if (d < r + 20) a.world.spawnParticle(Particle.ASH, p.getLocation().add(0, 2, 0), 12, 4, 2, 4, 0);
                }
            }

            @Override
            public void stop(Active a) {
                // el fuego que queda se apaga: los bloques se restauran desde el libro de cambios
            }
        };
    }
}
