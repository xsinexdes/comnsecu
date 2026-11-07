package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Consecuencias PERMANENTES: maldiciones del mundo que solo activa el admin (menú, comando o un disparador de evento)
 * y que duran hasta que se levantan (o hasta que pasan N días). Se guardan en permanentes.yml.
 */
public class Permanents implements Listener {
    public record Def(String id, String name, Material icon, String color, String desc) {
    }

    public static final List<Def> DEFS = List.of(
            new Def("noche_eterna", "Noche Eterna", Material.COAL_BLOCK, "§8", "El sol no vuelve a salir. Siempre es de noche."),
            new Def("invierno_eterno", "Invierno Eterno", Material.PACKED_ICE, "§b", "Tormenta de nieve perpetua: el frío congela a quien no lleve abrigo térmico."),
            new Def("sequia_eterna", "Sequía Eterna", Material.DEAD_BUSH, "§e", "Cielo despejado para siempre, cultivos casi sin crecer, el sol agota a la gente."),
            new Def("lluvia_eterna", "Lluvia Eterna", Material.WATER_BUCKET, "§9", "Tormenta sin fin: rayos, barro y caminar a duras penas."),
            new Def("hambruna", "Hambruna", Material.ROTTEN_FLESH, "§6", "El hambre aprieta el doble: todos pierden comida constantemente."),
            new Def("tierra_baldia", "Tierra Baldía", Material.COARSE_DIRT, "§7", "Los cultivos casi no crecen y los animales no crían."),
            new Def("plaga_eterna", "Plaga Eterna", Material.ZOMBIE_HEAD, "§2", "Los monstruos aparecen en grupos y más fuertes."),
            new Def("maldicion_cristal", "Maldición de Cristal", Material.GLASS, "§d", "Todos reciben un 30% más de daño. Todo es frágil."),
            new Def("niebla_perpetua", "Niebla Perpetua", Material.GRAY_STAINED_GLASS_PANE, "§7", "Una bruma espesa cubre el mundo; a campo abierto se oscurece la vista."),
            new Def("temblores", "Temblores", Material.POINTED_DRIPSTONE, "§c", "El suelo tiembla cada pocos minutos (efecto visual y sonoro).")
    );

    private final ConsecuenciasPlugin pl;
    private final File file;
    private final Map<String, Long> on = new LinkedHashMap<>(); // id -> caducidad en ms (0 = para siempre)
    private final Random rnd = new Random();
    private long nextQuake;
    private int beat;

    public Permanents(ConsecuenciasPlugin pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "permanentes.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(pl, this::tick, 40L, 20L);
    }

    public static Def def(String id) {
        for (Def d : DEFS) if (d.id().equalsIgnoreCase(id)) return d;
        return null;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        var sec = y.getConfigurationSection("activos");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) if (def(k) != null) on.put(k, sec.getLong(k, 0));
        if (!on.isEmpty()) pl.getLogger().info("Permanentes activas: " + on.keySet());
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (var e : on.entrySet()) y.set("activos." + e.getKey(), e.getValue());
        try {
            y.save(file);
        } catch (IOException ex) {
            pl.getLogger().warning("No se pudo guardar permanentes.yml: " + ex.getMessage());
        }
    }

    public boolean active(String id) {
        Long t = on.get(id);
        return t != null && (t == 0 || t > System.currentTimeMillis());
    }

    /** Milisegundos que le quedan (-1 = para siempre, 0 = no activa). */
    public long left(String id) {
        Long t = on.get(id);
        if (t == null) return 0;
        if (t == 0) return -1;
        return Math.max(0, t - System.currentTimeMillis());
    }

    public Map<String, Long> all() {
        return on;
    }

    /** Activa una maldición permanente. days = 0 → hasta que la levantes. false si ya estaba activa o no existe. */
    public boolean enable(String id, int days, String by) {
        Def d = def(id);
        if (d == null || active(d.id())) return false;
        on.put(d.id(), days > 0 ? System.currentTimeMillis() + days * 86_400_000L : 0L);
        save();
        pl.history.add("PERMANENTE ON " + d.name() + " | " + (days > 0 ? days + " días" : "para siempre") + " | por " + by);
        String when = days > 0 ? "durante " + days + " días" : "PARA SIEMPRE (hasta que se rompa)";
        for (World w : pl.manager.worlds()) for (Player p : w.getPlayers()) {
            p.sendMessage("§8§m" + " ".repeat(54));
            p.sendMessage("");
            p.sendMessage("   " + d.color() + "§l☠  CONSECUENCIA PERMANENTE: " + d.name().toUpperCase() + "  ☠");
            p.sendMessage("   §7" + d.desc());
            p.sendMessage("   §c" + when);
            p.sendMessage("");
            p.sendMessage("§8§m" + " ".repeat(54));
            p.sendTitle(d.color() + "§l" + d.name(), "§cConsecuencia permanente", 10, 80, 30);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.6f);
        }
        Webhook.send(pl, "☠ CONSECUENCIA PERMANENTE: " + d.name() + " — " + d.desc() + " (" + when + ")");
        return true;
    }

    public boolean disable(String id, String by) {
        Def d = def(id);
        if (d == null || on.remove(d.id()) == null) return false;
        save();
        pl.history.add("PERMANENTE OFF " + d.name() + " | por " + by);
        for (World w : pl.manager.worlds()) {
            if (d.id().equals("noche_eterna") && !active("noche_eterna")) {
                w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                w.setTime(1000);
            }
            for (Player p : w.getPlayers()) {
                p.sendMessage("§a✦ §7La consecuencia permanente §f" + d.name() + " §7ha sido levantada.");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
        }
        Webhook.send(pl, "✦ Se ha levantado la consecuencia permanente: " + d.name());
        return true;
    }

    // ------------------------------------------------------------------ bucle (1 s)
    private void tick() {
        beat++;
        long now = System.currentTimeMillis();
        for (String id : new java.util.ArrayList<>(on.keySet())) {
            long t = on.get(id);
            if (t != 0 && t <= now) disable(id, "caducó");
        }
        if (on.isEmpty()) return;
        for (World w : pl.manager.worlds()) {
            if (active("noche_eterna") && (w.getTime() < 13000 || w.getTime() > 23000)) {
                w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                w.setTime(18000);
            }
            boolean storm = active("invierno_eterno") || active("lluvia_eterna");
            boolean clear = active("sequia_eterna") && !storm;
            if (storm && (!w.hasStorm() || beat % 60 == 0)) {
                w.setStorm(true);
                w.setThundering(active("lluvia_eterna"));
                w.setWeatherDuration(6000);
            } else if (clear && (w.hasStorm() || beat % 60 == 0)) {
                w.setStorm(false);
                w.setClearWeatherDuration(6000);
            }
            for (Player p : w.getPlayers()) {
                if (!Util.valid(p)) continue;
                boolean out = Util.exposed(p);
                Location l = p.getLocation();
                if (active("invierno_eterno") && out && !Gear.hasCoat(p)) {
                    p.setFreezeTicks(Math.min(p.getMaxFreezeTicks(), p.getFreezeTicks() + 30));
                    w.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 1.5, 0), 20, 4, 2, 4, 0.02);
                } else if (active("invierno_eterno") && out) {
                    w.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 1.5, 0), 8, 4, 2, 4, 0.02);
                }
                if (active("lluvia_eterna") && out) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false, true));
                    if (beat % 40 == 0 && rnd.nextInt(100) < 35) w.strikeLightningEffect(Util.ring(l, 14, 34, rnd));
                }
                if (active("sequia_eterna") && out && w.getTime() < 12500) {
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, l.clone().add(rnd.nextInt(7) - 3, 0.5, rnd.nextInt(7) - 3), 0, 0, 0.05, 0, 1);
                    if (beat % 20 == 0) p.setFoodLevel(Math.max(0, p.getFoodLevel() - 1));
                }
                if (active("hambruna") && beat % 40 == 0) p.setFoodLevel(Math.max(0, p.getFoodLevel() - 1));
                if (active("niebla_perpetua")) {
                    w.spawnParticle(Particle.CLOUD, l.clone().add(0, 1, 0), 14, 6, 1.5, 6, 0.005);
                    if (out && beat % 25 == 0) p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, false, false, true));
                }
            }
        }
        if (active("temblores")) {
            if (nextQuake == 0) nextQuake = now + (3 + rnd.nextInt(4)) * 60_000L;
            if (now >= nextQuake) {
                nextQuake = now + (3 + rnd.nextInt(4)) * 60_000L;
                for (World w : pl.manager.worlds()) quake(w);
            }
        }
    }

    /** Temblor visual: sonido, polvo, y sacudida (sin tocar bloques). */
    private void quake(World w) {
        for (Player p : w.getPlayers()) {
            if (!Util.valid(p)) continue;
            p.sendTitle("§c§l¡TEMBLOR!", "§7El suelo se sacude...", 0, 40, 20);
            for (int i = 0; i < 12; i++) {
                final int k = i;
                Bukkit.getScheduler().runTaskLater(pl, () -> {
                    if (!p.isOnline()) return;
                    Location l = p.getLocation();
                    p.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.35f, 0.5f + 0.05f * k);
                    p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector((rnd.nextDouble() - 0.5) * 0.15, 0.12, (rnd.nextDouble() - 0.5) * 0.15)));
                    p.getWorld().spawnParticle(Particle.BLOCK, l.clone().add(0, 0.2, 0), 30, 3, 0.1, 3, 0.05,
                            l.clone().subtract(0, 1, 0).getBlock().getType().isSolid() ? l.clone().subtract(0, 1, 0).getBlock().getBlockData() : Material.DIRT.createBlockData());
                }, k * 6L);
            }
        }
    }

    // ------------------------------------------------------------------ eventos
    private boolean managed(World w) {
        return pl.manager.worlds().contains(w);
    }

    private static boolean crop(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOTS || m == Material.NETHER_WART
                || m == Material.SWEET_BERRY_BUSH || m == Material.MELON_STEM || m == Material.PUMPKIN_STEM || m == Material.COCOA
                || m == Material.SUGAR_CANE || m == Material.BAMBOO || m == Material.CACTUS || m == Material.KELP;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) {
        if (on.isEmpty() || !managed(e.getBlock().getWorld()) || !crop(e.getBlock().getType())) return;
        int chance = 0;
        if (active("sequia_eterna")) chance = Math.max(chance, 75);
        if (active("tierra_baldia")) chance = Math.max(chance, 85);
        if (active("invierno_eterno")) chance = Math.max(chance, 60);
        if (chance > 0 && rnd.nextInt(100) < chance) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent e) {
        if (active("tierra_baldia") && managed(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (active("tierra_baldia") && managed(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!active("plaga_eterna") || !(e.getEntity() instanceof Monster m) || !managed(m.getWorld())) return;
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        m.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
        m.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        if (rnd.nextInt(100) < 35) {
            Location l = m.getLocation();
            Bukkit.getScheduler().runTask(pl, () -> {
                if (!m.isValid()) return;
                LivingEntity extra = (LivingEntity) l.getWorld().spawnEntity(l, m.getType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
                extra.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!active("maldicion_cristal") || !(e.getEntity() instanceof Player p) || !managed(p.getWorld())) return;
        e.setDamage(e.getDamage() * 1.3);
    }
}
