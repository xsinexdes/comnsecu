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
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Vida: gas, grisú, infestación, peste, langostas, hambruna, peste ganadera, invasión zombi, desgarro, marea tóxica. */
public final class LifeDisasters {
    private LifeDisasters() {
    }

    public static void register(DisasterManager m) {
        m.register(gas());
        m.register(grisu());
        m.register(infestacion());
        m.register(peste());
        m.register(langostas());
        m.register(hambruna());
        m.register(pesteGanado());
        m.register(invasion());
        m.register(desgarro());
        m.register(marea());
    }

    private static final class Pocket {
        Location loc;
        long until;
        double r;
    }

    /** Crea bolsas de gas cerca de jugadores bajo tierra. */
    @SuppressWarnings("unchecked")
    private static void spawnPockets(Active a, int chance) {
        List<Pocket> list = (List<Pocket>) a.data.computeIfAbsent("bolsas", k -> new ArrayList<Pocket>());
        list.removeIf(p -> p.until < System.currentTimeMillis());
        for (Player p : a.players()) {
            if (!Util.underground(p) || list.size() >= 4 * a.level || a.rnd.nextInt(100) >= chance) continue;
            for (int t = 0; t < 8; t++) {
                Block b = p.getLocation().getBlock().getRelative(a.rnd.nextInt(13) - 6, a.rnd.nextInt(5) - 1, a.rnd.nextInt(13) - 6);
                if (!b.getType().isAir()) continue;
                Pocket pk = new Pocket();
                pk.loc = b.getLocation().add(0.5, 0.5, 0.5);
                pk.r = 3 + a.level;
                pk.until = System.currentTimeMillis() + (40 + a.rnd.nextInt(35)) * 1000L;
                list.add(pk);
                a.world.playSound(pk.loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.4f);
                break;
            }
        }
    }

    // ------------------------------------------------------------------ Gas venenoso
    private static Disaster gas() {
        return new Disaster("gas_venenoso", "Gas venenoso en cuevas", "Vida", Material.FERMENTED_SPIDER_EYE, 8, "§a",
                "Bolsas de gas tóxico aparecen bajo tierra: envenenan y marean. Mejor salir de las minas.") {
            @Override
            public String warning() {
                return "Bolsas de gas tóxico se abren en las cuevas. Subid a la superficie.";
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                spawnPockets(a, 22 * a.level);
                List<Pocket> list = (List<Pocket>) a.data.get("bolsas");
                if (list == null) return;
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(80, 210, 60), 1.8f);
                for (Pocket pk : list) {
                    a.world.spawnParticle(Particle.DUST, pk.loc, 30, pk.r / 2, pk.r / 3, pk.r / 2, dust);
                    for (Player p : a.players()) {
                        double d = p.getLocation().distance(pk.loc);
                        if (d <= pk.r) {
                            if (Gear.airSafe(p)) {
                                Gear.wearMask(p, 2);
                                Util.actionbar(p, "§a🛡 Tu máscara/antídoto te protege del gas");
                            } else {
                                D.eff(p, PotionEffectType.POISON, 80, a.level > 2 ? 1 : 0);
                                D.eff(p, PotionEffectType.NAUSEA, 120, 0);
                                Util.actionbar(p, "§a☣ ¡Respiras gas venenoso! §7¡Sal de ahí! §8(máscara antigás o antídoto)");
                            }
                        } else if (d <= pk.r + 6) Util.actionbar(p, "§a☣ Huele a gas...");
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Grisú
    private static Disaster grisu() {
        return new Disaster("grisu", "Grisú explosivo", "Vida", Material.TORCH, 6, "§6",
                "Bolsas de gas inflamable: explotan si hay fuego o antorchas cerca. Minar a oscuras.") {
            @Override
            public String warning() {
                return "Gas inflamable en las minas: ¡no llevéis antorchas ni fuego!";
            }

            private boolean fire(Block c, double r) {
                int ri = (int) Math.ceil(r);
                for (int x = -ri; x <= ri; x++) for (int y = -ri; y <= ri; y++) for (int z = -ri; z <= ri; z++) {
                    Material t = c.getRelative(x, y, z).getType();
                    if (t == Material.FIRE || t == Material.TORCH || t == Material.WALL_TORCH || t == Material.LANTERN || t == Material.LAVA
                            || t == Material.CAMPFIRE || t == Material.SOUL_TORCH || t == Material.SOUL_WALL_TORCH || t == Material.SOUL_LANTERN
                            || t == Material.FURNACE || t == Material.BLAST_FURNACE) return true;
                }
                return false;
            }

            private boolean lit(ItemStack it) {
                if (it == null) return false;
                Material t = it.getType();
                return t == Material.TORCH || t == Material.SOUL_TORCH || t == Material.LANTERN || t == Material.SOUL_LANTERN || t == Material.FLINT_AND_STEEL || t == Material.FIRE_CHARGE || t == Material.CAMPFIRE;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                spawnPockets(a, 20 * a.level);
                List<Pocket> list = (List<Pocket>) a.data.get("bolsas");
                if (list == null) return;
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(190, 150, 70), 1.6f);
                for (Iterator<Pocket> it = list.iterator(); it.hasNext(); ) {
                    Pocket pk = it.next();
                    a.world.spawnParticle(Particle.DUST, pk.loc, 20, pk.r / 2, pk.r / 3, pk.r / 2, dust);
                    boolean boom = false;
                    for (Player p : a.players()) {
                        double d = p.getLocation().distance(pk.loc);
                        if (d <= pk.r + 1 && (lit(p.getInventory().getItemInMainHand()) || lit(p.getInventory().getItemInOffHand()))) boom = true;
                        if (d <= pk.r + 5) Util.actionbar(p, "§6⚠ Grisú cerca: §7¡guarda las antorchas!");
                    }
                    if (!boom && a.beat % 20 == 0) boom = fire(pk.loc.getBlock(), pk.r);
                    if (boom) {
                        a.world.createExplosion(pk.loc, 2.2f + 0.6f * a.level, false, false);
                        a.world.spawnParticle(Particle.EXPLOSION_EMITTER, pk.loc, 1);
                        it.remove();
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Infestación subterránea
    private static Disaster infestacion() {
        return new Disaster("infestacion", "Infestación subterránea", "Vida", Material.SPIDER_EYE, 6, "§2",
                "Las profundidades se llenan de criaturas hambrientas. Minar se vuelve peligroso.") {
            @Override
            public void tick(Active a) {
                for (Player p : a.players()) {
                    if (!Util.underground(p) || D.mobs(a) >= 5 * a.level || a.rnd.nextInt(100) >= 38) continue;
                    for (int t = 0; t < 6; t++) {
                        Block b = p.getLocation().getBlock().getRelative(a.rnd.nextInt(17) - 8, a.rnd.nextInt(5) - 2, a.rnd.nextInt(17) - 8);
                        if (!b.getType().isAir() || !b.getRelative(0, 1, 0).getType().isAir() || !b.getRelative(0, -1, 0).getType().isSolid()) continue;
                        EntityType[] types = a.level >= 2
                                ? new EntityType[]{EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.SPIDER, EntityType.ZOMBIE}
                                : new EntityType[]{EntityType.CAVE_SPIDER, EntityType.SILVERFISH};
                        Entity e = a.world.spawnEntity(b.getLocation().add(0.5, 0, 0.5), types[a.rnd.nextInt(types.length)]);
                        Util.tag(e, a);
                        if (e instanceof Mob mob) mob.setTarget(p);
                        a.world.spawnParticle(Particle.SCULK_SOUL, e.getLocation().add(0, 0.5, 0), 6, 0.3, 0.3, 0.3, 0.02);
                        if (a.beat % 30 == 0) Util.actionbar(p, "§2🕷 Algo se mueve en la oscuridad...");
                        break;
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Peste
    private static Disaster peste() {
        return new Disaster("peste", "Peste", "Vida", Material.POISONOUS_POTATO, 10, "§2",
                "Una enfermedad se contagia entre jugadores. Se cura con leche o manzana dorada.") {
            @Override
            public String warning() {
                return "Una enfermedad se extiende. Evitad el contacto: la leche o una manzana dorada curan.";
            }

            @Override
            public void start(Active a) {
                a.data.put("infectados", new HashSet<UUID>());
                a.data.put("inmunes", new HashMap<UUID, Long>());
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                Set<UUID> inf = (Set<UUID>) a.data.get("infectados");
                Map<UUID, Long> inm = (Map<UUID, Long>) a.data.get("inmunes");
                long now = System.currentTimeMillis();
                List<Player> ps = a.players();
                if (a.beat % 50 == 0) {
                    // nuevo contagio espontáneo
                    if (!ps.isEmpty() && a.rnd.nextInt(100) < 25 * a.level) {
                        Player p = ps.get(a.rnd.nextInt(ps.size()));
                        if (!inf.contains(p.getUniqueId()) && inm.getOrDefault(p.getUniqueId(), 0L) < now && !shielded(a, p)) infect(a, inf, p);
                    }
                    // contagio por cercanía
                    for (Player s : ps) {
                        if (!inf.contains(s.getUniqueId())) continue;
                        for (Player o : ps) {
                            if (o == s || inf.contains(o.getUniqueId()) || inm.getOrDefault(o.getUniqueId(), 0L) > now || shielded(a, o)) continue;
                            if (o.getWorld() == s.getWorld() && o.getLocation().distanceSquared(s.getLocation()) < 16 && a.rnd.nextInt(100) < 35) infect(a, inf, o);
                        }
                    }
                }
                for (Player p : ps) {
                    if (!inf.contains(p.getUniqueId())) continue;
                    D.eff(p, PotionEffectType.WEAKNESS, 80, 0);
                    D.eff(p, PotionEffectType.HUNGER, 60, 0);
                    if (a.level >= 2) D.eff(p, PotionEffectType.SLOWNESS, 60, 0);
                    if (a.beat % 60 == 0) D.eff(p, PotionEffectType.NAUSEA, 80, 0);
                    if (a.level >= 3 && a.beat % 100 == 0) D.eff(p, PotionEffectType.POISON, 60, 0);
                    a.world.spawnParticle(Particle.DUST, p.getLocation().add(0, 1.4, 0), 5, 0.3, 0.4, 0.3, new Particle.DustOptions(Color.fromRGB(100, 160, 40), 1.2f));
                    if (a.beat % 20 == 0) Util.actionbar(p, "§2☣ Estás enfermo: §7bebe leche o come una manzana dorada");
                }
                inf.removeIf(u -> org.bukkit.Bukkit.getPlayer(u) == null);
            }

            private boolean shielded(Active a, Player p) {
                return Gear.immune(p) || (Gear.hasMask(p) && a.rnd.nextInt(100) < 80);
            }

            private void infect(Active a, Set<UUID> inf, Player p) {
                inf.add(p.getUniqueId());
                p.sendMessage("§2☣ §7Te has contagiado de la §2peste§7. Bebe leche o come una manzana dorada para curarte, y aléjate de los demás.");
                p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1f, 0.6f);
            }
        };
    }

    // ------------------------------------------------------------------ Langostas
    private static Disaster langostas() {
        return new Disaster("langostas", "Plaga de langostas", "Vida", Material.WHEAT, 6, "§e",
                "Enjambres devoran los cultivos. Los campos quedan pelados.") {
            @Override
            public String warning() {
                return "Enjambres de langostas se acercan a los campos. ¡Proteged las cosechas!";
            }

            @Override
            public int every() {
                return 5;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                List<Location> sw = (List<Location>) a.data.computeIfAbsent("enjambres", k -> new ArrayList<Location>());
                List<Player> ps = a.players();
                if (!ps.isEmpty() && sw.size() < 2 * a.level && a.rnd.nextInt(100) < 20) {
                    Location l = Util.ring(ps.get(a.rnd.nextInt(ps.size())).getLocation(), 10, 30, a.rnd);
                    Block t = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                    if (t != null) sw.add(t.getLocation().add(0.5, 2.5, 0.5));
                }
                boolean grief = D.pl().getConfig().getBoolean("destruccion", true);
                for (Iterator<Location> it = sw.iterator(); it.hasNext(); ) {
                    Location l = it.next();
                    l.add(a.rnd.nextDouble() * 2 - 1, 0, a.rnd.nextDouble() * 2 - 1);
                    Block t = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                    if (t == null) {
                        it.remove();
                        continue;
                    }
                    l.setY(t.getY() + 2.5);
                    a.world.spawnParticle(Particle.DUST, l, 40, 2.5, 1.2, 2.5, new Particle.DustOptions(Color.fromRGB(95, 70, 25), 1.0f));
                    if (a.beat % 10 == 0) a.world.playSound(l, Sound.ENTITY_BEE_LOOP, 2f, 0.6f);
                    if (grief) {
                        for (int i = 0; i < 40; i++) {
                            Block b = a.world.getBlockAt(l.getBlockX() + a.rnd.nextInt(13) - 6, l.getBlockY() + a.rnd.nextInt(5) - 4, l.getBlockZ() + a.rnd.nextInt(13) - 6);
                            if (Tag.CROPS.isTagged(b.getType()) || b.getType() == Material.MELON_STEM || b.getType() == Material.PUMPKIN_STEM || b.getType() == Material.SWEET_BERRY_BUSH) {
                                D.pl().ledger.set(b, Material.AIR, a);
                            }
                        }
                    }
                    if (a.rnd.nextInt(100) < 3) it.remove();
                }
                for (Player p : ps) {
                    for (Location l : sw) if (l.distanceSquared(p.getLocation()) < 400 && a.beat % 40 == 0) {
                        Util.actionbar(p, "§e🦗 Un enjambre de langostas cerca...");
                        break;
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Hambruna
    private static Disaster hambruna() {
        return new Disaster("hambruna", "Hambruna", "Vida", Material.BREAD, 12, "§e",
                "Los cultivos no crecen, el ganado no cría y la comida alimenta la mitad.") {
            @Override
            public String warning() {
                return "Escasea la comida: los cultivos no crecerán y comer alimenta menos. Racionad.";
            }

            @Override
            public int every() {
                return 20;
            }

            @Override
            public void tick(Active a) {
                for (Player p : a.players()) {
                    p.setExhaustion(p.getExhaustion() + 0.8f * a.level);
                    if (a.beat % 400 == 0) Util.actionbar(p, "§e🍞 Hambruna: §7cada bocado alimenta menos");
                }
            }
        };
    }

    // ------------------------------------------------------------------ Peste ganadera
    private static Disaster pesteGanado() {
        return new Disaster("peste_ganado", "Peste ganadera", "Vida", Material.ROTTEN_FLESH, 6, "§4",
                "Los animales de granja enferman y mueren. No se pueden criar. Las mascotas están a salvo.") {
            @Override
            public int every() {
                return 20;
            }

            @Override
            public void tick(Active a) {
                if (!D.pl().getConfig().getBoolean("desastres.peste_ganado.matar", true)) return;
                for (Player p : a.world.getPlayers()) {
                    int n = 0;
                    for (Entity e : p.getNearbyEntities(48, 24, 48)) {
                        if (!(e instanceof Animals an) || n >= 14) continue;
                        if (e instanceof AbstractHorse || e instanceof Tameable t && t.isTamed() || e.customName() != null || an.isLeashed()) continue;
                        n++;
                        a.world.spawnParticle(Particle.DUST, an.getLocation().add(0, 0.8, 0), 6, 0.3, 0.3, 0.3, new Particle.DustOptions(Color.fromRGB(90, 130, 30), 1.1f));
                        if (a.rnd.nextInt(100) < 35 * a.level) an.damage(1.5 * a.level);
                    }
                    if (n > 0 && Util.valid(p) && a.beat % 200 == 0) Util.actionbar(p, "§4☠ El ganado enferma a tu alrededor");
                }
            }
        };
    }

    // ------------------------------------------------------------------ Invasión zombi
    private static Disaster invasion() {
        return new Disaster("invasion_zombi", "Invasión zombi", "Vida", Material.ZOMBIE_HEAD, 8, "§2",
                "Oleadas de zombis cada vez más grandes asaltan a los jugadores, también de día.") {
            @Override
            public String warning() {
                return "Oleadas de no-muertos marchan hacia vosotros. Fortificaos.";
            }

            @Override
            public void start(Active a) {
                a.data.put("oleada", 0);
            }

            @Override
            public void tick(Active a) {
                if (a.beat % 200 != 0 && !(a.beat == 10)) return;
                int wave = a.get("oleada", 0) + 1;
                a.data.put("oleada", wave);
                com.etheria.consecuencias.Announcer.news(a, "🧟 Oleada §f" + wave + " §7de la invasión zombi.");
                int cap = 22 + 8 * a.level;
                for (Player p : a.players()) {
                    int n = Math.min(10, 2 + wave + a.level);
                    for (int i = 0; i < n && D.mobs(a) < cap; i++) {
                        Location l = Util.ring(p.getLocation(), 16, 30, a.rnd);
                        Block b = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                        if (b == null || b.isLiquid()) continue;
                        Zombie z = a.world.spawn(b.getLocation().add(0.5, 1, 0.5), Zombie.class, zz -> {
                            Util.tag(zz, a);
                            zz.setTarget(p);
                            zz.setCanBreakDoors(true);
                            if (wave >= 2 && a.rnd.nextInt(100) < 12) zz.setBaby();
                            if (wave >= 3) zz.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                            if (wave >= 5) zz.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                            if (wave >= 7) zz.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                            zz.getEquipment().setHelmetDropChance(0.05f);
                            zz.getEquipment().setChestplateDropChance(0.05f);
                            zz.getEquipment().setItemInMainHandDropChance(0.05f);
                        });
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.5f);
                }
            }
        };
    }

    // ------------------------------------------------------------------ Desgarro dimensional
    private static Disaster desgarro() {
        return new Disaster("desgarro", "Desgarro dimensional", "Vida", Material.CRYING_OBSIDIAN, 6, "§5",
                "Grietas al vacío se abren, atraen a los jugadores y escupen criaturas de otras dimensiones.") {
            @Override
            public String warning() {
                return "La realidad se resquebraja. Mantened la distancia de las grietas púrpuras.";
            }

            @Override
            public int every() {
                return 4;
            }

            private void openRift(Active a, List<Object[]> rifts) {
                List<Player> ps = a.players();
                if (ps.isEmpty()) return;
                Location l = Util.ring(ps.get(a.rnd.nextInt(ps.size())).getLocation(), 14, 34, a.rnd);
                Block b = Util.top(a.world, l.getBlockX(), l.getBlockZ());
                if (b == null) return;
                rifts.add(new Object[]{b.getLocation().add(0.5, 2, 0.5), System.currentTimeMillis() + (50 + a.rnd.nextInt(40)) * 1000L});
                com.etheria.consecuencias.Announcer.news(a, "🌀 Se abre un desgarro cerca de §f" + b.getX() + ", " + b.getZ() + "§7.");
            }

            @Override
            @SuppressWarnings("unchecked")
            public void tick(Active a) {
                List<Object[]> rifts = (List<Object[]>) a.data.computeIfAbsent("grietas", k -> new ArrayList<Object[]>());
                long now = System.currentTimeMillis();
                rifts.removeIf(r -> (Long) r[1] < now);
                while (rifts.size() < a.level) {
                    int before = rifts.size();
                    openRift(a, rifts);
                    if (rifts.size() == before) break;
                }
                for (Object[] r : rifts) {
                    Location c = (Location) r[0];
                    a.world.spawnParticle(Particle.PORTAL, c, 80, 0.4, 1.4, 0.4, 0.6);
                    a.world.spawnParticle(Particle.REVERSE_PORTAL, c, 20, 1.2, 1.4, 1.2, 0.05);
                    if (a.beat % 20 == 0) a.world.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 2f, 0.6f);
                    for (Player p : a.players()) {
                        double d = p.getLocation().distance(c);
                        if (d < 7) {
                            Vector pull = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.18);
                            p.setVelocity(p.getVelocity().add(pull));
                            if (d < 1.8) {
                                p.damage(2);
                                D.eff(p, PotionEffectType.DARKNESS, 60, 0);
                            }
                        }
                    }
                    if (a.beat % 20 == 0 && D.mobs(a) < 6 * a.level) {
                        EntityType[] types = a.level >= 3
                                ? new EntityType[]{EntityType.ENDERMITE, EntityType.ENDERMAN, EntityType.VEX, EntityType.PIGLIN_BRUTE}
                                : a.level == 2 ? new EntityType[]{EntityType.ENDERMITE, EntityType.ENDERMAN, EntityType.VEX}
                                : new EntityType[]{EntityType.ENDERMITE, EntityType.ENDERMAN};
                        Entity e = a.world.spawnEntity(c.clone().add(0, -1, 0), types[a.rnd.nextInt(types.length)]);
                        Util.tag(e, a);
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------ Marea tóxica
    private static Disaster marea() {
        return new Disaster("marea_toxica", "Marea tóxica", "Vida", Material.PUFFERFISH, 8, "§c",
                "El agua se vuelve roja y venenosa: envenena a los nadadores y mata a los peces.") {
            @Override
            public String warning() {
                return "Las aguas se tiñen de rojo. No nadéis ni bebáis de ellas.";
            }

            @Override
            public void tick(Active a) {
                Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(200, 40, 40), 1.6f);
                for (Player p : a.world.getPlayers()) {
                    D.columns(a, p, 6, 28, top -> {
                        if (top.getType() == Material.WATER) a.world.spawnParticle(Particle.DUST, top.getLocation().add(0.5, 1.05, 0.5), 5, 0.5, 0.05, 0.5, red);
                    });
                    if (Util.valid(p) && Util.inWater(p) && !Gear.immune(p)) {
                        D.eff(p, PotionEffectType.POISON, 80, 0);
                        D.eff(p, PotionEffectType.NAUSEA, 100, 0);
                        Util.actionbar(p, "§c☠ ¡El agua está envenenada! §7Sal de ella");
                    }
                    int n = 0;
                    for (Entity e : p.getNearbyEntities(40, 20, 40)) {
                        if (n >= 16) break;
                        EntityType t = e.getType();
                        if (t == EntityType.COD || t == EntityType.SALMON || t == EntityType.TROPICAL_FISH || t == EntityType.PUFFERFISH || t == EntityType.SQUID || t == EntityType.GLOW_SQUID) {
                            n++;
                            if (a.rnd.nextInt(100) < 40 * a.level && e instanceof LivingEntity le) le.damage(2);
                        }
                    }
                }
            }
        };
    }
}
